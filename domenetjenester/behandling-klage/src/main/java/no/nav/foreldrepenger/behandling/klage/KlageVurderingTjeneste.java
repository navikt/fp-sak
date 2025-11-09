package no.nav.foreldrepenger.behandling.klage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingRelasjonEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingBehandlingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;

@ApplicationScoped
public class KlageVurderingTjeneste {

    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private KlageRepository klageRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingEventPubliserer behandlingEventPubliserer;
    private MottatteDokumentRepository mottatteDokumentRepository;

    @Inject
    public KlageVurderingTjeneste(DokumentBestillerTjeneste dokumentBestillerTjeneste,
                                  DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  KlageRepository klageRepository,
                                  BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                  BehandlingsresultatRepository behandlingsresultatRepository,
                                  BehandlingEventPubliserer behandlingEventPubliserer,
                                  MottatteDokumentRepository mottatteDokumentRepository) {
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.klageRepository = klageRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingEventPubliserer = behandlingEventPubliserer;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    KlageVurderingTjeneste() {
        // for CDI proxy
    }

    public static boolean skalBehandlesAvKlageInstans(KlageVurdertAv klageVurdertAv, KlageVurdering klageVurdering) {
        return KlageVurdertAv.NFP.equals(klageVurdertAv) && KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(klageVurdering);
    }

    public KlageResultatEntitet hentEvtOpprettKlageResultat(Behandling behandling) {
        return klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
    }

    public Optional<KlageResultatEntitet> hentKlageResultatHvisEksisterer(Behandling behandling) {
        return klageRepository.hentKlageResultatHvisEksisterer(behandling.getId());
    }

    public Optional<KlageFormkravEntitet> hentKlageFormkrav(Behandling klageBehandling, KlageVurdertAv vurdertAv) {
        return klageRepository.hentKlageFormkrav(klageBehandling.getId(), vurdertAv);
    }

    public KlageFormkravEntitet.Builder hentKlageFormkravBuilder(Behandling klageBehandling, KlageVurdertAv vurdertAv) {
        var eksisterende = klageRepository.hentKlageFormkrav(klageBehandling.getId(), vurdertAv);
        return eksisterende.map(KlageFormkravEntitet::builder).orElse(KlageFormkravEntitet.builder()).medKlageVurdertAv(vurdertAv);
    }

    public void oppdaterKlageMedPåklagetBehandling(Behandling klageBehandling, UUID påklagetBehandlingUuid) {
        var påklagetBehandlingId = getPåklagetBehandlingId(påklagetBehandlingUuid);
        klageRepository.settPåklagdBehandlingId(klageBehandling.getId(), påklagetBehandlingId.orElse(null));
        behandlingEventPubliserer.publiserBehandlingEvent(new BehandlingRelasjonEvent(klageBehandling));
    }

    public void oppdaterKlageMedKabalReferanse(Long klageBehandlingId, String ref) {
        klageRepository.settKabalReferanse(klageBehandlingId, ref);
    }

    private Optional<Long> getPåklagetBehandlingId(UUID påklagetBehandlingUuid) {
        if (påklagetBehandlingUuid == null) {
            return Optional.empty();
        }
        var påklagetBehandling = behandlingRepository.hentBehandling(påklagetBehandlingUuid);
        return Optional.of(påklagetBehandling.getId());
    }

    public void oppdaterKlageMedPåklagetEksternBehandlingUuid(Long klageBehandlingId, UUID påklagetEksternBehandlingUuid) {
        klageRepository.settPåklagdEksternBehandlingUuid(klageBehandlingId, påklagetEksternBehandlingUuid);
    }

    public void lagreFormkrav(Behandling behandling, KlageFormkravEntitet.Builder builder) {
        klageRepository.lagreFormkrav(behandling, builder);
    }

    public Optional<KlageVurderingResultat> hentKlageVurderingResultat(Behandling behandling, KlageVurdertAv vurdertAv) {
        return klageRepository.hentKlageVurderingResultat(behandling.getId(), vurdertAv);
    }

    public KlageVurderingResultat.Builder hentKlageVurderingResultatBuilder(Behandling behandling, KlageVurdertAv vurdertAv) {
        var eksisterende = klageRepository.hentKlageVurderingResultat(behandling.getId(), vurdertAv);
        return eksisterende.map(KlageVurderingResultat::builder).orElse(KlageVurderingResultat.builder()).medKlageVurdertAv(vurdertAv);
    }

    public void oppdaterBekreftetVurderingAksjonspunkt(Behandling behandling, BehandlingLås lås, KlageVurderingResultat.Builder builder, KlageVurdertAv vurdertAv) {
        lagreKlageVurderingResultat(behandling, lås, builder, vurdertAv, true);
    }

    public void lagreKlageVurderingResultat(Behandling behandling, BehandlingLås lås, KlageVurderingResultat.Builder builder, KlageVurdertAv vurdertAv) {
        lagreKlageVurderingResultat(behandling, lås, builder, vurdertAv, false);
    }

    private void lagreKlageVurderingResultat(Behandling behandling, BehandlingLås lås,
                                             KlageVurderingResultat.Builder builder, KlageVurdertAv vurdertAv,
                                             boolean erVurderingOppdaterer) {
        var aksjonspunkt = KlageVurdertAv.NK.equals(vurdertAv) ? null : AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP;
        var vurderingsteg = KlageVurdertAv.NK.equals(vurdertAv) ? BehandlingStegType.KLAGE_NK : BehandlingStegType.KLAGE_NFP;

        var klageResultat = hentEvtOpprettKlageResultat(behandling);
        var nyttresultat = builder.medKlageResultat(klageResultat).medKlageVurdertAv(vurdertAv).build();
        var eksisterende = hentKlageVurderingResultat(behandling, vurdertAv).orElse(null);

        var uendret = eksisterende != null && eksisterende.harLikVurdering(nyttresultat);
        var endretBeslutterStatus = eksisterende != null && !uendret;
        var kabal = klageResultat.erBehandletAvKabal();

        var tilbakeføres = !kabal && endretBeslutterStatus && !behandling.harÅpentAksjonspunktMedType(aksjonspunkt)
            && behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandling, vurderingsteg);
        klageRepository.lagreVurderingsResultat(behandling.getId(), nyttresultat);
        if (erVurderingOppdaterer || tilbakeføres || kabal) {
            settBehandlingResultatTypeBasertPaaUtfall(behandling, nyttresultat.getKlageVurdering(), nyttresultat.getKlageVurderingOmgjør(),
                vurdertAv);
        }
        if (tilbakeføres) {
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
            tilbakeførBehandling(behandling, lås, vurderingsteg);
        }
    }

    private void tilbakeførBehandling(Behandling behandling, BehandlingLås lås, BehandlingStegType vurderingSteg) {
        behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, vurderingSteg);
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
    }

    private void settBehandlingResultatTypeBasertPaaUtfall(Behandling behandling, KlageVurdering klageVurdering, KlageVurderingOmgjør omgjør, KlageVurdertAv vurdertAv) {
        if (skalBehandlesAvKlageInstans(vurdertAv, klageVurdering) && !dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.KLAGE_OVERSENDT)) {
            var dokumentBestilling = DokumentBestilling.builder()
                .medBehandlingUuid(behandling.getUuid())
                .medSaksnummer(behandling.getSaksnummer())
                .medDokumentMal(DokumentMalType.KLAGE_OVERSENDT)
                .build();
            dokumentBestillerTjeneste.bestillDokument(dokumentBestilling);
            oppdaterBehandlingMedNyFrist(behandling);
        }
        var klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        var erPåklagdEksternBehandling = klageResultatEntitet.getPåKlagdBehandlingId().isEmpty()
                && klageResultatEntitet.getPåKlagdEksternBehandlingUuid().isPresent();
        var behandlingResultatType = KlageVurderingBehandlingResultat.tolkBehandlingResultatType(klageVurdering, omgjør, erPåklagdEksternBehandling);

        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        if (behandlingsresultat.isPresent()) {
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat.get())
                    .medBehandlingResultatType(behandlingResultatType);
        } else {
            Behandlingsresultat.builder()
                    .medBehandlingResultatType(behandlingResultatType)
                    .buildFor(behandling);
        }
    }

    private void oppdaterBehandlingMedNyFrist(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingstidFrist(LocalDate.now().plusWeeks(14));
        behandlingRepository.lagre(behandling, lås);
    }

    public static String historikkResultatForKlageVurdering(KlageVurdering vurdering, KlageVurdertAv vurdertAv, KlageVurderingOmgjør klageVurderingOmgjør) {
        if (KlageVurdering.AVVIS_KLAGE.equals(vurdering)) {
            return "Klagen er avvist";
        }
        if (KlageVurdering.MEDHOLD_I_KLAGE.equals(vurdering)) {
            if (KlageVurderingOmgjør.DELVIS_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
                return "Vedtaket er delvis omgjort";
            }
            if (KlageVurderingOmgjør.UGUNST_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
                return "Vedtaket er omgjort til ugunst";
            }
            return "Vedtaket er omgjort";
        }
        if (KlageVurdering.OPPHEVE_YTELSESVEDTAK.equals(vurdering)) {
            return "Vedtaket er opphevet";
        }
        if (KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE.equals(vurdering)) {
            return "Behandling er hjemsendt";
        }
        if (KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(vurdering)) {
            if (KlageVurdertAv.NFP.equals(vurdertAv)) {
                return "Vedtaket er opprettholdt";
            }
            return "Vedtaket er stadfestet";
        }
        return null;
    }

    public Optional<LocalDate> getKlageMottattDato(Behandling behandling) {
        return hentKlageFormkrav(behandling, KlageVurdertAv.NFP).map(KlageFormkravEntitet::getMottattDato)
            .or(() -> getKlageDokumentMottattDato(behandling));
    }

    private Optional<LocalDate> getKlageDokumentMottattDato(Behandling behandling) {
        return mottatteDokumentRepository.hentMottatteDokument(behandling.getId()).stream()
            .filter(dok -> DokumentTypeId.KLAGE_DOKUMENT.equals(dok.getDokumentType()))
            .map(MottattDokument::getMottattDato)
            .min(Comparator.naturalOrder());
    }
}
