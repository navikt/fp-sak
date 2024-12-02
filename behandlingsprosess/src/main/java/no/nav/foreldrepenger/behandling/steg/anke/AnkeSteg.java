package no.nav.foreldrepenger.behandling.steg.anke;

import java.util.Comparator;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandling.kabal.SendTilKabalTask;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@BehandlingStegRef(BehandlingStegType.ANKE)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class AnkeSteg implements BehandlingSteg {

    private static final Logger LOG = LoggerFactory.getLogger(AnkeSteg.class);

    private KlageRepository klageRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private AnkeVurderingTjeneste ankeVurderingTjeneste;

    public AnkeSteg() {
        // For CDI proxy
    }

    @Inject
    public AnkeSteg(AnkeVurderingTjeneste ankeVurderingTjeneste, KlageRepository klageRepository, ProsessTaskTjeneste prosessTaskTjeneste,
                    BehandlingRepositoryProvider behandlingRepositoryProvider ) {
        this.klageRepository = klageRepository;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        /*
         * Ved første besøk kan anke være opprettet manuelt i VL eller pga opprettet-anke-hendelse fra KABAL med referanse
         * Ved senere besøk kan hoppet tilbake, ta av kabal-vent uten resultat, eller avsluttet-anke-hendelse fra KABAL med referanse
         * - Anke opprettet i VL, uten kabalhendelse -> overfør til kabal manuelt/automatisk/begge - tenk steg + oppdaterer. Sett på vent
         * - Anke opprettet i KABAL og behandling i VL med referanse -> settes på vent til behandling i Kabal avsluttet
         * - Anke avsluttet / trukket -> henlegges utenfor steg
         * - Anke avsluttet / retur -> ukjent betydning, exception utenfor steg
         * - Anke avsluttet / andre utfall -> fortsett/avslutt uten flere AP.
        */
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var klageId = ankeVurderingTjeneste.hentAnkeResultatHvisEksisterer(behandling)
            .flatMap(AnkeResultatEntitet::getPåAnketKlageBehandlingId)
            .or(() -> utledLagrePåanketKlageBehandling(behandling));
        var kabalReferanse = ankeVurderingTjeneste.hentAnkeResultatHvisEksisterer(behandling)
            .map(AnkeResultatEntitet::erBehandletAvKabal).orElse(false);
        var harVentKabal = behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE);

        if (kabalReferanse) { // Skal ikke oversendes
            // Første gang med kabalRef -> vent på kabal
            // Tatt av vent med kabalref -> har mottatt resultat fra kabal. gå videre
            if (!harVentKabal || manglerAnkeVurdering(behandling)) {
                return BehandleStegResultat.utførtMedAksjonspunktResultat(ventPåKabal());
            } else {
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }
        }

        if (!harVentKabal) { // Har ikke vært sent til kabal, send over.
            var hjemmel = klageId.flatMap(k -> klageRepository.hentKlageVurderingResultat(k, KlageVurdertAv.NFP))
                .map(KlageVurderingResultat::getKlageHjemmel)
                .filter(h -> !KlageHjemmel.UDEFINERT.equals(h))
                .orElseGet(() -> KlageHjemmel.standardHjemmelForYtelse(behandling.getFagsakYtelseType()));
            var tilKabalTask = ProsessTaskData.forProsessTask(SendTilKabalTask.class);
            tilKabalTask.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
            tilKabalTask.setCallIdFraEksisterende();
            tilKabalTask.setProperty(SendTilKabalTask.HJEMMEL_KEY, hjemmel.getKode());
            prosessTaskTjeneste.lagre(tilKabalTask);
        }
        return BehandleStegResultat.utførtMedAksjonspunktResultat(ventPåKabal());
    }

    private AksjonspunktResultat ventPåKabal() {
        return AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE, Venteårsak.VENT_KABAL, null);
    }

    private boolean manglerAnkeVurdering(Behandling anke) {
        var ankeVurdering = ankeVurderingTjeneste.hentAnkeVurderingResultat(anke);
        return ankeVurdering.isEmpty() || ankeVurdering.map(AnkeVurderingResultatEntitet::getAnkeVurdering)
            .filter(AnkeVurdering.UDEFINERT::equals)
            .isPresent();
    }

    private Optional<Long> utledLagrePåanketKlageBehandling(Behandling anke) {
        var aktuelleKlager = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(anke.getFagsakId()).stream()
            .filter(b -> BehandlingType.KLAGE.equals(b.getType()))
            .filter(Behandling::erSaksbehandlingAvsluttet)
            .filter(b -> behandlingsresultatRepository.hentHvisEksisterer(b.getId()).filter(br -> !br.isBehandlingHenlagt()).isPresent())
            .toList();
        if (aktuelleKlager.size() == 1) {
            return Optional.of(lagrePåanketBehandling(anke, aktuelleKlager.get(0)));
        } else if (aktuelleKlager.size() > 1) {
            var utvalgtKlage = aktuelleKlager.stream()
                .filter(k -> klageRepository.hentKlageResultatHvisEksisterer(k.getId()).filter(KlageResultatEntitet::erBehandletAvKabal).isPresent())
                .max(Comparator.comparing(Behandling::getAvsluttetDato))
                .or(() -> aktuelleKlager.stream()
                    .filter(k -> klageRepository.hentGjeldendeKlageVurderingResultat(k).filter(kvr -> KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(kvr.getKlageVurdering())).isPresent())
                    .max(Comparator.comparing(Behandling::getAvsluttetDato)))
                .orElseGet(() -> aktuelleKlager.stream().max(Comparator.comparing(Behandling::getAvsluttetDato)).orElseThrow());
            return Optional.of(lagrePåanketBehandling(anke, utvalgtKlage));
        }
        LOG.warn("ANKE steg: kunne ikke utlede klagebehandling automatisk sak {} anke {}", anke.getSaksnummer().getVerdi(), anke.getId());
        return Optional.empty();
    }

    private Long lagrePåanketBehandling(Behandling anke, Behandling klage) {
        ankeVurderingTjeneste.oppdaterAnkeMedPåanketKlage(anke, klage.getId());
        return klage.getId();
    }
}
