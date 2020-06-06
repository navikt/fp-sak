package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste;


import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.TERMIN;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamiliehendelseEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class EtterkontrollEventObserver {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private EtterkontrollRepository etterkontrollRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;
    private Period etterkontrollTidTilbake;

    public EtterkontrollEventObserver() {
        //Cool Devices Installed
    }

    /**
     * @param etterkontrollTidTilbake - Tid etter innvilgelsesdato før en fagsak vurderes for etterkontroll
     */
    @Inject
    public EtterkontrollEventObserver(EtterkontrollRepository etterkontrollRepository,
                                      FamilieHendelseRepository familieHendelseRepository,
                                      BehandlingRepository behandlingRepository,
                                      @KonfigVerdi(value = "etterkontroll.tid.tilbake", defaultVerdi = "P60D") Period etterkontrollTidTilbake) {
        this.etterkontrollRepository = etterkontrollRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.behandlingRepository = behandlingRepository;
        this.etterkontrollTidTilbake = etterkontrollTidTilbake;
    }


    public void observerFamiliehendelseEvent(@Observes FamiliehendelseEvent event) {
        log.debug("Mottatt familehendelseEvent for behandling {} ", event.getBehandlingId());//NOSONAR
        if (FamiliehendelseEvent.EventType.TERMIN_TIL_FØDSEL.equals(event.getEventType())) {
            etterkontrollRepository.avflaggDersomEksisterer(event.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
        }
    }

    public void observerBehandlingVedtakEvent(@Observes BehandlingVedtakEvent event) {
        Behandling behandling = event.getBehandling();
        if (!IverksettingStatus.IVERKSATT.equals(event.getVedtak().getIverksettingStatus()) || !behandling.erYtelseBehandling()) {
            return;
        }

        log.debug("Markerer behandling {} for etterkontroll på bakgrunn av opprettet vedtak {} om ytelse knyttet til termin", event.getBehandlingId(), event.getVedtak().getId());//NOSONAR
        final Optional<FamilieHendelseGrunnlagEntitet> grunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        if (grunnlag.isPresent()) {
            final FamilieHendelseType hendelseType = grunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
            if (Set.of(TERMIN, FØDSEL).contains(hendelseType)) {
                markerForEtterkontroll(behandling, grunnlag.get());
            }
        }
        if (Set.of(VedtakResultatType.AVSLAG, VedtakResultatType.OPPHØR).contains(event.getVedtak().getVedtakResultatType())) {
            etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
        }
    }

    private void markerForEtterkontroll(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag) {
        skalEtterkontrolleresMedDato(behandling, grunnlag).ifPresent(ekDato -> {
            var ekTid = ekDato.plus(etterkontrollTidTilbake).atStartOfDay();
            List<Etterkontroll> ekListe = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
            if (ekListe.isEmpty()) {
                Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId())
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
        // Markerer for etterkontroll alle som mangler register-data, bekreftet. Etterkontroll-batch håndterer logikk for overstyring og ES vs FP
        if (!Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.ENGANGSTØNAD).contains(behandling.getFagsak().getYtelseType())) {
            return Optional.empty();
        }
        if (familieHendelseGrunnlag.getBekreftetVersjon().map(FamilieHendelseEntitet::getType).map(FamilieHendelseType.FØDSEL::equals).orElse(false)) {
            return Optional.empty();
        }
        return Optional.of(familieHendelseGrunnlag.finnGjeldendeFødselsdato());
    }
}
