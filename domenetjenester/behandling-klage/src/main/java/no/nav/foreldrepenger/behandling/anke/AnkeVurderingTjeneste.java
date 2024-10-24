package no.nav.foreldrepenger.behandling.anke;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingBehandlingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingRelasjonEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class AnkeVurderingTjeneste {

    private AnkeRepository ankeRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingEventPubliserer behandlingEventPubliserer;

    AnkeVurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AnkeVurderingTjeneste(BehandlingRepository behandlingRepository,
                                 BehandlingsresultatRepository behandlingsresultatRepository,
                                 AnkeRepository ankeRepository,
                                 BehandlingEventPubliserer behandlingEventPubliserer) {
        this.ankeRepository = ankeRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingEventPubliserer = behandlingEventPubliserer;
    }

    public Optional<AnkeResultatEntitet> hentAnkeResultatHvisEksisterer(Behandling behandling) {
        return ankeRepository.hentAnkeResultat(behandling.getId());
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

    public void oppdaterAnkeMedKabalReferanse(Long ankeBehandlingId, String ref) {
        ankeRepository.settKabalReferanse(ankeBehandlingId, ref);
    }

    public void oppdaterAnkeMedPåanketKlage(Behandling ankeBehandling, Long klageBehandlingId) {
        ankeRepository.settPåAnketKlageBehandling(ankeBehandling.getId(), klageBehandlingId);
        behandlingEventPubliserer.publiserBehandlingEvent(new BehandlingRelasjonEvent(ankeBehandling));
    }

    public void oppdaterBekreftetVurderingAksjonspunkt(Behandling behandling,
                                                       AnkeVurderingResultatEntitet.Builder builder) {
        lagreAnkeVurderingResultat(behandling, builder);
    }

    private void lagreAnkeVurderingResultat(Behandling behandling, AnkeVurderingResultatEntitet.Builder builder) {
        var ankeResultat = hentAnkeResultat(behandling);
        var nyttresultat = builder.medAnkeResultat(ankeResultat).build();
        ankeRepository.lagreVurderingsResultat(behandling.getId(), nyttresultat);
        var nyVurdering = nyttresultat.getTrygderettVurdering() == null || AnkeVurdering.UDEFINERT.equals(nyttresultat.getTrygderettVurdering()) ?
            nyttresultat.getAnkeVurdering() : nyttresultat.getTrygderettVurdering();
        var nyOmgjør = nyttresultat.getTrygderettVurdering() == null || AnkeVurdering.UDEFINERT.equals(nyttresultat.getTrygderettVurdering()) ?
            nyttresultat.getAnkeVurderingOmgjør() : nyttresultat.getTrygderettVurderingOmgjør();
        settBehandlingResultatTypeBasertPaaUtfall(behandling, nyVurdering, nyOmgjør);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    private void settBehandlingResultatTypeBasertPaaUtfall(Behandling behandling, AnkeVurdering ankeVurdering, AnkeVurderingOmgjør omgjør) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        var behandlingResultatType = AnkeVurderingBehandlingResultat.tolkBehandlingResultatType(ankeVurdering, omgjør);
        if (behandlingsresultat != null) {
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                    .medBehandlingResultatType(behandlingResultatType);
        } else {
            Behandlingsresultat.builder()
                    .medBehandlingResultatType(behandlingResultatType)
                    .buildFor(behandling);
        }
    }

    public BehandlingResultatType oppdatertBehandlingsResultat(Behandling b) {
        var avr = ankeRepository.hentAnkeVurderingResultat(b.getId()).orElseThrow();
        return AnkeVurderingBehandlingResultat.tolkBehandlingResultatType(avr.getTrygderettVurdering(), avr.getTrygderettVurderingOmgjør());
    }

    public static HistorikkResultatType konverterAnkeVurderingTilResultatType(AnkeVurdering vurdering, AnkeVurderingOmgjør ankeVurderingOmgjør) {
        if (AnkeVurdering.ANKE_AVVIS.equals(vurdering)) {
            return HistorikkResultatType.ANKE_AVVIS;
        }
        if (AnkeVurdering.ANKE_OMGJOER.equals(vurdering)) {
            if (AnkeVurderingOmgjør.ANKE_DELVIS_OMGJOERING_TIL_GUNST.equals(ankeVurderingOmgjør)) {
                return HistorikkResultatType.ANKE_DELVIS_OMGJOERING_TIL_GUNST;
            }
            if (AnkeVurderingOmgjør.ANKE_TIL_UGUNST.equals(ankeVurderingOmgjør)) {
                return HistorikkResultatType.ANKE_TIL_UGUNST;
            }
            if (AnkeVurderingOmgjør.ANKE_TIL_GUNST.equals(ankeVurderingOmgjør)) {
                return HistorikkResultatType.ANKE_TIL_GUNST;
            }
            return HistorikkResultatType.ANKE_OMGJOER;
        }
        if (AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE.equals(vurdering)) {
            return HistorikkResultatType.ANKE_OPPHEVE_OG_HJEMSENDE;
        }
        if (AnkeVurdering.ANKE_HJEMSEND_UTEN_OPPHEV.equals(vurdering)) {
            return HistorikkResultatType.ANKE_HJEMSENDE;
        }
        if (AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK.equals(vurdering)) {
            return HistorikkResultatType.ANKE_STADFESTET_VEDTAK;
        }
        return null;
    }
}
