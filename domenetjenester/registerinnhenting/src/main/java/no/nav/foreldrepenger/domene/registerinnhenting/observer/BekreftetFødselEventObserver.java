package no.nav.foreldrepenger.domene.registerinnhenting.observer;


import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.events.FamiliehendelseEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.RegisterinnhentingHistorikkinnslagTjeneste;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;

@ApplicationScoped
public class BekreftetFødselEventObserver {

    protected final Logger LOG = LoggerFactory.getLogger(BekreftetFødselEventObserver.class);

    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private BehandlingRepository behandlingRepository;

    public BekreftetFødselEventObserver() {
        //Cool Devices Installed
    }

    /**
     *
     */
    @Inject
    public BekreftetFødselEventObserver(BehandlingRepositoryProvider repositoryProvider,
                                        RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    }

    public void observerFamiliehendelseEvent(@Observes FamiliehendelseEvent event) {
        if (FamiliehendelseEvent.EventType.TERMIN_TIL_FØDSEL.equals(event.getEventType()) &&
            FagsakYtelseType.FORELDREPENGER.equals(event.getYtelseType()) &&
            event.getSisteBekreftetDato() != null) {
            var brukFødselsdato = VirkedagUtil.fomVirkedag(event.getSisteBekreftetDato());
            vurderNyStartdato(behandlingRepository.hentBehandling(event.getBehandlingId()), brukFødselsdato);
        }
    }

    private void vurderNyStartdato(Behandling behandling, LocalDate gjeldendeFødselsdato) {
        if (RelasjonsRolleType.MORA.equals(behandling.getFagsak().getRelasjonsRolleType())) {
            getNåværendeAvklartStartdato(behandling)
                .filter(gjeldendeFødselsdato::isBefore)
                .ifPresent(avklartStartdato -> {
                    LOG.info("Fødselshendelse behandlingId {}: Bekreftet fødsel {} er før avklart startdato {}, nullstiller avklart startdato",
                        behandling.getId(), gjeldendeFødselsdato, avklartStartdato);
                    nullstillStartdatoOgOpprettHistorikkInnslag(behandling, avklartStartdato, gjeldendeFødselsdato);
            });
        }
    }

    private Optional<LocalDate> getNåværendeAvklartStartdato(Behandling behandling) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId())
            .flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);
    }

    private void nullstillStartdatoOgOpprettHistorikkInnslag(Behandling behandling, LocalDate gammelStartdato, LocalDate nyBekreftetFødselsdato) {
        var avklarteDatoer = ytelsesFordelingRepository.hentAggregat(behandling.getId()).getAvklarteDatoer();

        var entitet = new AvklarteUttakDatoerEntitet.Builder(avklarteDatoer).medFørsteUttaksdato(null);

        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medAvklarteDatoer(entitet.build());
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        historikkinnslagTjeneste.opprettHistorikkinnslagForEndretStartdatoEtterFødselshendelse(behandling, gammelStartdato, nyBekreftetFødselsdato);
    }
}
