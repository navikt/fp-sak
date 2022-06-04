package no.nav.foreldrepenger.behandling.steg.anke;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.kabal.SendTilKabalTask;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
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
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
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

    private AnkeRepository ankeRepository;
    private KlageRepository klageRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    public AnkeSteg() {
        // For CDI proxy
    }

    @Inject
    public AnkeSteg(AnkeRepository ankeRepository, KlageRepository klageRepository, ProsessTaskTjeneste prosessTaskTjeneste,
                    BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.ankeRepository = ankeRepository;
        this.klageRepository = klageRepository;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var klageId = ankeRepository.hentAnkeResultat(kontekst.getBehandlingId())
            .flatMap(AnkeResultatEntitet::getPåAnketKlageBehandlingId)
            .or(() -> utledLagrePåanketKlageBehandling(behandling));
        if (klageId.isEmpty()) {
            // TODO håndtere flere/ingen klager - obs oversendelse
            BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE));
        }
        /**
         * TODO: Gå opp dynamikk rundt kabal. Må håndtere en del scenarier
         * - Anke opprettet i VL, uten kabalhendelse -> overfør til kabal manuelt/automatisk/begge - tenk steg + oppdaterer. Sett på vent
         * - Anke opprettet i KABAL og behandling i VL med referanse -> settes på vent til behandling i Kabal avsluttet
         * - Anke avsluttet / trukket -> henlegges utenfor steg
         * - Anke avsluttet / retur -> ukjent betydning, exception utenfor steg
         * - Anke avsluttet / andre utfall -> fortsett/avslutt uten flere AP.
         *
        */
        // Ved første besøk kan anke være opprettet manuelt i VL eller pga opprettet-anke-hendelse fra KABAL med referanse
        // Ved senere besøk kan hoppet tilbake, ta av kabal-vent uten resultat, eller avsluttet-anke-hendelse fra KABAL med referanse
        var kabalReferanse = ankeRepository.hentAnkeResultat(kontekst.getBehandlingId())
            .map(AnkeResultatEntitet::erBehandletAvKabal).orElse(false);
        var harVentKabal = behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE);
        var erOpprettetAvKabal = kabalReferanse && !harVentKabal;

        if (kabalReferanse) { // Skal ikke oversendes
            // Første gang med kabalRef -> reset kabalreferanse, vent på kabal
            if (erOpprettetAvKabal) {
                ankeRepository.settKabalReferanse(behandling.getId(), null);
            }
            // Tatt av vent med kabalref -> har mottatt resultat fra kabal. gå videre
            if (!harVentKabal || manglerAnkeVurdering(behandling.getId())) {
                return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(ventPåKabal()));
            } else {
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }
        }
        if (!harVentKabal) { // Hos Kabal, mangler utfall
            var hjemmel = klageId.flatMap(k -> klageRepository.hentKlageVurderingResultat(k, KlageVurdertAv.NFP))
                .map(KlageVurderingResultat::getKlageHjemmel)
                .filter(h -> !KlageHjemmel.UDEFINERT.equals(h))
                .orElseGet(() -> KlageHjemmel.standardHjemmelForYtelse(behandling.getFagsakYtelseType()));
            var tilKabalTask = ProsessTaskData.forProsessTask(SendTilKabalTask.class);
            tilKabalTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            tilKabalTask.setCallIdFraEksisterende();
            tilKabalTask.setProperty(SendTilKabalTask.HJEMMEL_KEY, hjemmel.getKode());
            prosessTaskTjeneste.lagre(tilKabalTask);
        }
        return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(ventPåKabal()));
    }

    private AksjonspunktResultat ventPåKabal() {
        return AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE, Venteårsak.VENT_KABAL, null);
    }

    private boolean manglerAnkeVurdering(Long behandlingId) {
        var ankeVurdering = ankeRepository.hentAnkeVurderingResultat(behandlingId);
        return ankeVurdering.isEmpty() || ankeVurdering.map(AnkeVurderingResultatEntitet::getAnkeVurdering)
            .filter(AnkeVurdering.UDEFINERT::equals)
            .isPresent();
    }

    private Optional<Long> utledLagrePåanketKlageBehandling(Behandling anke) {
        var aktuelleKlager = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(anke.getFagsakId()).stream()
            .filter(b -> BehandlingType.KLAGE.equals(b.getType()))
            .filter(Behandling::erSaksbehandlingAvsluttet)
            .filter(b -> behandlingsresultatRepository.hentHvisEksisterer(b.getId()).filter(br -> !br.isBehandlingHenlagt()).isPresent())
            .collect(Collectors.toList());
        // TODO vurder å velge sist avsluttet elns dersom flere klager
        if (aktuelleKlager.size() == 1) {
            var klageId = aktuelleKlager.get(0).getId();
            ankeRepository.settPåAnketKlageBehandling(anke.getId(), klageId);
            return Optional.of(klageId);
        }
        return Optional.empty();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
            BehandlingStegType sisteSteg) {
        ankeRepository.settAnkeGodkjentHosMedunderskriver(kontekst.getBehandlingId(), false);
    }
}
