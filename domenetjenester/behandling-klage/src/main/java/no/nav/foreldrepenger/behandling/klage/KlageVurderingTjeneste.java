package no.nav.foreldrepenger.behandling.klage;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;

@ApplicationScoped
public class KlageVurderingTjeneste {

    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;
    private KlageRepository klageRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public KlageVurderingTjeneste(DokumentBestillerTjeneste dokumentBestillerTjeneste,
                                  ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  KlageRepository klageRepository,
                                  BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                  BehandlingsresultatRepository behandlingsresultatRepository) {
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.klageRepository = klageRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    KlageVurderingTjeneste() {
        // for CDI proxy
    }

    public KlageResultatEntitet hentEvtOpprettKlageResultat(Behandling behandling) {
        return klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
    }

    public Optional<KlageFormkravEntitet> hentKlageFormkrav(Behandling klageBehandling, KlageVurdertAv vurdertAv) {
        return klageRepository.hentKlageFormkrav(klageBehandling.getId(), vurdertAv);
    }

    public KlageFormkravEntitet.Builder hentKlageFormkravBuilder(Behandling klageBehandling, KlageVurdertAv vurdertAv) {
        var eksisterende = klageRepository.hentKlageFormkrav(klageBehandling.getId(), vurdertAv);
        return eksisterende.map(KlageFormkravEntitet::builder).orElse(KlageFormkravEntitet.builder()).medKlageVurdertAv(vurdertAv);
    }

    public void oppdaterKlageMedPåklagetBehandling(Long klageBehandlingId, Long påklagetBehandlingId) {
        klageRepository.settPåklagdBehandlingId(klageBehandlingId, påklagetBehandlingId);
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

    public Optional<KlageVurderingResultat> hentGjeldendeKlageVurderingResultat(Behandling behandling) {
        return klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
    }

    public KlageVurderingResultat.Builder hentKlageVurderingResultatBuilder(Behandling behandling, KlageVurdertAv vurdertAv) {
        var eksisterende = klageRepository.hentKlageVurderingResultat(behandling.getId(), vurdertAv);
        return eksisterende.map(KlageVurderingResultat::builder).orElse(KlageVurderingResultat.builder()).medKlageVurdertAv(vurdertAv);
    }

    public void oppdaterBekreftetVurderingAksjonspunkt(Behandling behandling, KlageVurderingResultat.Builder builder, KlageVurdertAv vurdertAv) {
        lagreKlageVurderingResultat(behandling, builder, vurdertAv, true);
    }

    public void lagreKlageVurderingResultat(Behandling behandling, KlageVurderingResultat.Builder builder, KlageVurdertAv vurdertAv) {
        lagreKlageVurderingResultat(behandling, builder, vurdertAv, false);
    }

    private void lagreKlageVurderingResultat(Behandling behandling, KlageVurderingResultat.Builder builder, KlageVurdertAv vurdertAv,
            boolean erVurderingOppdaterer) {
        var aksjonspunkt = KlageVurdertAv.NK.equals(vurdertAv) ? AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NK
                : AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP;
        var vurderingsteg = KlageVurdertAv.NK.equals(vurdertAv) ? BehandlingStegType.KLAGE_NK : BehandlingStegType.KLAGE_NFP;

        var klageResultat = hentEvtOpprettKlageResultat(behandling);
        var nyttresultat = builder.medKlageResultat(klageResultat).medKlageVurdertAv(vurdertAv).build();
        var eksisterende = hentKlageVurderingResultat(behandling, vurdertAv).orElse(null);

        var uendret = (eksisterende != null) && eksisterende.harLikVurdering(nyttresultat);
        var endretBeslutterStatus = (eksisterende != null) && eksisterende.isGodkjentAvMedunderskriver() && !uendret;

        if (eksisterende == null) {
            nyttresultat.setGodkjentAvMedunderskriver(false);
        } else {
            nyttresultat.setGodkjentAvMedunderskriver(eksisterende.isGodkjentAvMedunderskriver() && uendret);
            if (endretBeslutterStatus && KlageVurdertAv.NFP.equals(vurdertAv)) {
                klageRepository.settKlageGodkjentHosMedunderskriver(behandling.getId(), KlageVurdertAv.NK, false);
            }
        }
        var tilbakeføres = endretBeslutterStatus &&
                !behandling.harÅpentAksjonspunktMedType(aksjonspunkt) &&
                behandlingskontrollTjeneste.erStegPassert(behandling, vurderingsteg);
        klageRepository.lagreVurderingsResultat(behandling.getId(), nyttresultat);
        if (erVurderingOppdaterer || tilbakeføres) {
            settBehandlingResultatTypeBasertPaaUtfall(behandling, nyttresultat.getKlageVurdering(), vurdertAv);
        }
        if (tilbakeføres) {
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
            tilbakeførBehandling(behandling, vurderingsteg);
        }
    }

    private void tilbakeførBehandling(Behandling behandling, BehandlingStegType vurderingSteg) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, vurderingSteg);
        prosesseringAsynkTjeneste.asynkProsesserBehandling(behandling);
    }

    private void settBehandlingResultatTypeBasertPaaUtfall(Behandling behandling, KlageVurdering klageVurdering, KlageVurdertAv vurdertAv) {
        if (KlageVurdertAv.NFP.equals(vurdertAv) && klageVurdering.equals(KlageVurdering.STADFESTE_YTELSESVEDTAK)
                && !Fagsystem.INFOTRYGD.equals(behandling.getMigrertKilde())) {

            var bestillBrevDto = new BestillBrevDto(behandling.getId(), behandling.getUuid(), DokumentMalType.KLAGE_OVERSENDT_KLAGEINSTANS);
            dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER, false);
            oppdaterBehandlingMedNyFrist(behandling);
        }
        var klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        var erPåklagdEksternBehandling = klageResultatEntitet.getPåKlagdBehandlingId().isEmpty()
                && klageResultatEntitet.getPåKlagdEksternBehandlingUuid().isPresent();
        var behandlingResultatType = BehandlingResultatType.tolkBehandlingResultatType(klageVurdering, erPåklagdEksternBehandling);

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
}
