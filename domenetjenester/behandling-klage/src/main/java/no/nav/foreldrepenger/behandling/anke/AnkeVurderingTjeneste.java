package no.nav.foreldrepenger.behandling.anke;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;

@ApplicationScoped
public class AnkeVurderingTjeneste {
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;
    private AnkeRepository ankeRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    AnkeVurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AnkeVurderingTjeneste(ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                 BehandlingRepository behandlingRepository,
                                 BehandlingsresultatRepository behandlingsresultatRepository,
                                 AnkeRepository ankeRepository,
                                 BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.ankeRepository = ankeRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    public AnkeResultatEntitet hentAnkeResultat(Behandling behandling) {
        return ankeRepository.hentEllerOpprettAnkeResultat(behandling.getId());
    }

    public Optional<AnkeVurderingResultatEntitet> hentAnkeVurderingResultat(Behandling behandling) {
        return ankeRepository.hentAnkeVurderingResultat(behandling.getId());
    }

    public AnkeVurderingResultatEntitet.Builder hentAnkeVurderingResultatBuilder(Behandling behandling) {
        var eksisterende = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
        return eksisterende.map(AnkeVurderingResultatEntitet::builder).orElse(AnkeVurderingResultatEntitet.builder());
    }

    public void oppdaterBekreftetVurderingAksjonspunkt(Behandling behandling, AnkeVurderingResultatEntitet.Builder builder, Long påanketBehandlingId) {
        ankeRepository.settPåAnketBehandling(behandling.getId(), påanketBehandlingId);
        lagreAnkeVurderingResultat(behandling, builder, true);
    }

    public void oppdaterBekreftetMerknaderAksjonspunkt(Behandling behandling, boolean erMerknaderMottatt, String merknadKommentar,
                                                       AnkeVurdering trVurdering, AnkeVurderingOmgjør trVurderOmgjør, AnkeOmgjørÅrsak trOmgjørÅrsak) {
        var builder = hentAnkeVurderingResultatBuilder(behandling)
            .medErMerknaderMottatt(erMerknaderMottatt)
            .medMerknaderFraBruker(merknadKommentar)
            .medTrygderettVurdering(trVurdering != null ? trVurdering : AnkeVurdering.UDEFINERT)
            .medTrygderettVurderingOmgjør(trVurderOmgjør != null ? trVurderOmgjør : AnkeVurderingOmgjør.UDEFINERT)
            .medTrygderettOmgjørÅrsak(trOmgjørÅrsak != null ? trOmgjørÅrsak : AnkeOmgjørÅrsak.UDEFINERT);
        ankeRepository.lagreVurderingsResultat(behandling.getId(), builder.build());
    }

    public void lagreAnkeVurderingResultat(Behandling behandling, AnkeVurderingResultatEntitet.Builder builder, Long påanketBehandlingId) {
        if (!behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE))
            throw new IllegalArgumentException("Utviklerfeil: Skal ikke kalle denne når aksjonspunkt er utført");
        ankeRepository.settPåAnketBehandling(behandling.getId(), påanketBehandlingId);
        lagreAnkeVurderingResultat(behandling, builder, false);
    }

    public void lagreAnkeVurderingResultat(Behandling behandling, AnkeVurderingResultatEntitet.Builder builder) {
        lagreAnkeVurderingResultat(behandling, builder, false);
    }

    private void lagreAnkeVurderingResultat(Behandling behandling, AnkeVurderingResultatEntitet.Builder builder, boolean erVurderingOppdaterer) {
        var ankeResultat = hentAnkeResultat(behandling);
        var nyttresultat = builder.medAnkeResultat(ankeResultat).build();
        var eksisterende = hentAnkeVurderingResultat(behandling).orElse(null);
        var endretBeslutterStatus = false;
        if (eksisterende == null) {
            nyttresultat.setGodkjentAvMedunderskriver(false);
        } else {
            var uendret = eksisterende.harLikVurdering(nyttresultat);
            endretBeslutterStatus = eksisterende.godkjentAvMedunderskriver() && !uendret;
            nyttresultat.setGodkjentAvMedunderskriver(eksisterende.godkjentAvMedunderskriver() && uendret);
        }
        var tilbakeføres = endretBeslutterStatus &&
            !behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE) &&
            behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.ANKE);
        ankeRepository.lagreVurderingsResultat(behandling.getId(), nyttresultat);
        if (erVurderingOppdaterer || tilbakeføres) {
            settBehandlingResultatTypeBasertPaaUtfall(behandling, nyttresultat.getAnkeVurdering());
        }
        if (tilbakeføres) {
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
            tilbakeførBehandling(behandling);
        }
    }

    private void tilbakeførBehandling(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.FORESLÅ_VEDTAK);
        prosesseringAsynkTjeneste.asynkProsesserBehandling(behandling);
    }

    private void settBehandlingResultatTypeBasertPaaUtfall(Behandling behandling, AnkeVurdering ankeVurdering) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        BehandlingResultatType behandlingResultatType = BehandlingResultatType.tolkBehandlingResultatType(ankeVurdering);
        if (behandlingsresultat != null) {
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                .medBehandlingResultatType(behandlingResultatType);
        } else {
            Behandlingsresultat.builder()
                .medBehandlingResultatType(behandlingResultatType)
                .buildFor(behandling);
        }
    }
}
