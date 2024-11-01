package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste;

import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.TERMIN;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.FamiliehendelseEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.Etterkontroll;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class EtterkontrollEventObserver {

    private static final Logger LOG = LoggerFactory.getLogger(EtterkontrollEventObserver.class);

    private EtterkontrollRepository etterkontrollRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private Period etterkontrollTidTilbake;

    public EtterkontrollEventObserver() {
        // Cool Devices Installed
    }

    /**
     * @param etterkontrollTidTilbake - Tid etter innvilgelsesdato før en fagsak
     *                                vurderes for etterkontroll
     */
    @Inject
    public EtterkontrollEventObserver(EtterkontrollRepository etterkontrollRepository,
            FamilieHendelseRepository familieHendelseRepository,
            @KonfigVerdi(value = "etterkontroll.tid.tilbake", defaultVerdi = "P60D") Period etterkontrollTidTilbake) {
        this.etterkontrollRepository = etterkontrollRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.etterkontrollTidTilbake = etterkontrollTidTilbake;
    }

    public void observerFamiliehendelseEvent(@Observes FamiliehendelseEvent event) {
        LOG.debug("Mottatt familehendelseEvent for behandling {} ", event.getBehandlingId());
        if (FamiliehendelseEvent.EventType.TERMIN_TIL_FØDSEL.equals(event.getEventType())) {
            etterkontrollRepository.avflaggDersomEksisterer(event.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
        }
    }

    public void observerBehandlingVedtakEvent(@Observes BehandlingVedtakEvent event) {
        var behandling = event.behandling();
        if (!event.iverksattYtelsesVedtak()) {
            return;
        }

        LOG.info("Etterkontroll Behandlingvedtakevent inngang behandling {}", event.getBehandlingId());

        var grunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        if (grunnlag.isPresent()) {
            var hendelseType = grunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseEntitet::getType)
                .orElse(FamilieHendelseType.UDEFINERT);
            if (Set.of(TERMIN, FØDSEL).contains(hendelseType)) {
                markerForEtterkontroll(behandling, grunnlag.get());
            }
        }
        if (Set.of(VedtakResultatType.AVSLAG, VedtakResultatType.OPPHØR).contains(event.vedtak().getVedtakResultatType())) {
            etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
        }
        LOG.info("Etterkontroll Behandlingvedtakevent utgang behandling {}", event.getBehandlingId());
    }

    private void markerForEtterkontroll(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag) {
        skalEtterkontrolleresMedDato(behandling, grunnlag).ifPresent(ekDato -> {
            var ekTid = ekDato.plus(etterkontrollTidTilbake).atStartOfDay();
            var ekListe = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
            if (ekListe.isEmpty()) {
                var etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId())
                        .medKontrollType(KontrollType.MANGLENDE_FØDSEL)
                        .medErBehandlet(false)
                        .medKontrollTidspunkt(ekTid)
                        .build();
                etterkontrollRepository.lagre(etterkontroll);
            } else {
                ekListe.forEach(ek -> {
                    ek.setKontrollTidspunktt(ekTid);
                    ek.setErBehandlet(false);
                    etterkontrollRepository.lagre(ek);
                });
            }
        });
    }

    private Optional<LocalDate> skalEtterkontrolleresMedDato(Behandling behandling, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        // Markerer for etterkontroll alle som mangler register-data, bekreftet.
        // Etterkontroll-batch håndterer logikk for overstyring og ES vs FP
        if (!Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.ENGANGSTØNAD).contains(behandling.getFagsak().getYtelseType())) {
            return Optional.empty();
        }
        if (familieHendelseGrunnlag.getGjeldendeBekreftetVersjon().map(FamilieHendelseEntitet::getType).filter(FamilieHendelseType.FØDSEL::equals).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(familieHendelseGrunnlag.finnGjeldendeFødselsdato());
    }
}
