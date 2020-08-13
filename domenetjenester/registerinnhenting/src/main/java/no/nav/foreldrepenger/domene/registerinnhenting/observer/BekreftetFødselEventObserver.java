package no.nav.foreldrepenger.domene.registerinnhenting.observer;


import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamiliehendelseEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.RegisterinnhentingHistorikkinnslagTjeneste;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class BekreftetFødselEventObserver {

    protected final Logger LOG = LoggerFactory.getLogger(BekreftetFødselEventObserver.class);

    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public BekreftetFødselEventObserver() {
        //Cool Devices Installed
    }

    /**
     *
     */
    @Inject
    public BekreftetFødselEventObserver(BehandlingRepositoryProvider repositoryProvider,
                                        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                        RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }


    public void observerFamiliehendelseEvent(@Observes FamiliehendelseEvent event) {
        if (FamiliehendelseEvent.EventType.TERMIN_TIL_FØDSEL.equals(event.getEventType()) && event.getSisteBekreftetDato() != null &&
            (event.getForrigeBekreftetDato() == null || event.getSisteBekreftetDato().isBefore(event.getForrigeBekreftetDato()))) {
            var brukFødselsdato = VirkedagUtil.fomVirkedag(event.getSisteBekreftetDato());
            vurderNyStartdato(behandlingRepository.hentBehandling(event.getBehandlingId()), brukFødselsdato);
        }
    }

    private void vurderNyStartdato(Behandling behandling, LocalDate gjeldendeFødselsdato) {
        if (RelasjonsRolleType.MORA.equals(behandling.getFagsak().getRelasjonsRolleType())) {
            var startDato = getNåværendeStartdato(behandling);
            if (gjeldendeFødselsdato.isBefore(startDato)) {
                LOG.info("Fødselshendelse behandlingId {}: Bekreftet fødsel er før startdato {}, setter avklart dato {}",
                    behandling.getId(), startDato, gjeldendeFødselsdato);
                settNyStartdatoOgOpprettHistorikkInnslag(behandling, gjeldendeFødselsdato);
            }
        }
    }

    private LocalDate getNåværendeStartdato(Behandling behandling) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId())
            .flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato)
            .orElseGet(() -> skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getFørsteUttaksdato());
    }

    private void settNyStartdatoOgOpprettHistorikkInnslag(Behandling behandling, LocalDate nyBekreftetFødselsdato) {
        final Optional<AvklarteUttakDatoerEntitet> avklarteDatoer = ytelsesFordelingRepository.hentAggregat(behandling.getId()).getAvklarteDatoer();
        final LocalDate gammelStartdato = avklarteDatoer.map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato).orElse(null);
        final AvklarteUttakDatoerEntitet entitet = new AvklarteUttakDatoerEntitet.Builder(avklarteDatoer).medFørsteUttaksdato(nyBekreftetFødselsdato).build();
        ytelsesFordelingRepository.lagre(behandling.getId(), entitet);
        historikkinnslagTjeneste.opprettHistorikkinnslagForEndretStartdatoEtterFødselshendelse(behandling, gammelStartdato, nyBekreftetFødselsdato);
    }
}
