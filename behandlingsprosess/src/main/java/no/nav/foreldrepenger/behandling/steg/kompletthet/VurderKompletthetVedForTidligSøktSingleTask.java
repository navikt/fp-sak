package no.nav.foreldrepenger.behandling.steg.kompletthet;

import static no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles.kanPassereKompletthet;
import static no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles.skalPassereKompletthet;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeFarRundtFødsel;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;


@ApplicationScoped
@ProsessTask(value = "vurderkompletthet.tidlig.søkt", maxFailedRuns = 1, prioritet = 3)
public class VurderKompletthetVedForTidligSøktSingleTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(VurderKompletthetVedForTidligSøktSingleTask.class);

    static final String BEHANDLING_ID = "behandlingId";
    static final String DRY_RUN = "dryRun";

    private Kompletthetsjekker kompletthetsjekker;
    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;


    VurderKompletthetVedForTidligSøktSingleTask() {
        // for CDI proxy
    }

    @Inject
    public VurderKompletthetVedForTidligSøktSingleTask(Kompletthetsjekker kompletthetsjekker, BehandlingRepository behandlingRepository, YtelsesFordelingRepository ytelsesFordelingRepository, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste, BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.kompletthetsjekker = kompletthetsjekker;
        this.behandlingRepository = behandlingRepository;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandlingId = Optional.ofNullable(prosessTaskData.getPropertyValue(BEHANDLING_ID)).map(Long::valueOf).orElseThrow();
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).map(Boolean::valueOf).orElse(Boolean.TRUE);
        var resultat = utførSteg(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);


        if (resultat.getAksjonspunktListe().contains(VENT_PGA_FOR_TIDLIG_SØKNAD) && !behandling.harÅpentAksjonspunktMedType(VENT_PGA_FOR_TIDLIG_SØKNAD)) {
            LOG.info("KOMPLETTHET_TIDLIG: Saksnummer {} skulle vært satt på vent pga for tidlig søknad men er i steg {} med følgende aksjonspunkt {}",
                behandling.getSaksnummer(),
                behandling.getAktivtBehandlingSteg().getKode(),
                behandling.getÅpneAksjonspunkter().stream().map(ap -> ap.getAksjonspunktDefinisjon().getKode()).toList());
            if (dryRun.equals(Boolean.FALSE)) {
                var lås = behandlingRepository.taSkriveLås(behandling);
                var fagsak = behandling.getFagsak();
                var kontekst = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), lås);

                if (!behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.VURDER_KOMPLETT_TIDLIG)) {
                    return;
                }
                if (behandling.isBehandlingPåVent()) {
                    behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT));
                }
                behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.VURDER_KOMPLETT_TIDLIG);

                if (behandling.isBehandlingPåVent()) {
                    behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT));
                }
                behandlingskontrollTjeneste.prosesserBehandling(kontekst);

                LOG.info("KOMPLETTHET_TIDLIG: Saksnummer {} flyttes fra steg {} til {}",
                    behandling.getSaksnummer(), behandling.getAktivtBehandlingSteg().getKode(), BehandlingStegType.VURDER_KOMPLETT_TIDLIG);
            }
        }
    }


    private BehandleStegResultat utførSteg(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (skalPassereKompletthet(behandling) || behandling.erRevurdering()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);

        if (!skalVenteDersomSøktForTidlig(behandling, skjæringstidspunkter)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var søknadMottatt = kompletthetsjekker.vurderSøknadMottattForTidlig(BehandlingReferanse.fra(behandling), skjæringstidspunkter);
        if (søknadMottatt.erOppfylt()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return VurderKompletthetStegFelles.evaluerUoppfylt(søknadMottatt, VENT_PGA_FOR_TIDLIG_SØKNAD);
    }

    private boolean skalVenteDersomSøktForTidlig(Behandling behandling, Skjæringstidspunkt stp) {
        //Hvis vi ikke kan passere kompletthet må vi uansett vente
        if (!kanPassereKompletthet(behandling)) {
            return true;
        }

        if (RelasjonsRolleType.erMor(behandling.getRelasjonsRolleType())) {
            return false;
        }
        var søknadFHDato = stp.getFamilieHendelseDato().map(FamilieHendelseDato::familieHendelseDato).orElse(null);
        var farRundtFødselTom = TidsperiodeFarRundtFødsel.intervallFarRundtFødsel(false, true, søknadFHDato, søknadFHDato)
                .map(LocalDateInterval::getTomDato).orElse(søknadFHDato);
        var farØnskerJustert = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId())
                .map(YtelseFordelingAggregat::getGjeldendeFordeling)
                .filter(OppgittFordelingEntitet::ønskerJustertVedFødsel)
                .filter(fordeling -> erFørsteUttaksdatoRundtFødsel(fordeling, farRundtFødselTom))
                .isPresent();
        return !farØnskerJustert;
    }

    private boolean erFørsteUttaksdatoRundtFødsel(OppgittFordelingEntitet oppgittFordeling, LocalDate fødselsperiodeTom) {
        return fødselsperiodeTom != null && oppgittFordeling.getPerioder().stream()
                .filter(periode -> !(periode.isUtsettelse() || periode.isOpphold()))
                .anyMatch(p -> p.getFom().isBefore(fødselsperiodeTom));
    }

}
