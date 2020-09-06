package no.nav.foreldrepenger.behandling.klage;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;

@ApplicationScoped
public class KlageVurderingTjeneste {

    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;
    private KlageRepository klageRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    KlageVurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KlageVurderingTjeneste(DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                                  ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  KlageRepository klageRepository,
                                  BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.klageRepository = klageRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public void oppdater(Behandling behandling, KlageVurderingAdapter adapter) {
        byggOgLagreKlageVurderingResultat(behandling, adapter);
        settBehandlingResultatTypeBasertPaaUtfall(behandling, adapter);
    }

    public void mellomlagreVurderingResultat(Behandling behandling, KlageVurderingAdapter adapter) {
        byggOgLagreKlageVurderingResultat(behandling, adapter);
    }

    public void mellomlagreVurderingResultatOgÅpneAksjonspunkt(Behandling behandling, KlageVurderingAdapter adapter) {
        tilbakeførBehandling(behandling, adapter);
        byggOgLagreKlageVurderingResultat(behandling, adapter);
    }

    private void tilbakeførBehandling(Behandling behandling, KlageVurderingAdapter adapter) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        BehandlingStegType stegType = adapter.getErNfpAksjonspunkt() ? BehandlingStegType.KLAGE_NFP : BehandlingStegType.KLAGE_NK;
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, stegType);
        prosesseringAsynkTjeneste.asynkProsesserBehandling(behandling);
    }

    private void byggOgLagreKlageVurderingResultat(Behandling behandling, KlageVurderingAdapter adapter) {
        KlageVurdering klageVurdering = adapter.getKlageVurderingKode() != null
            ? KlageVurdering.fraKode(adapter.getKlageVurderingKode())
            : null;
        KlageResultatEntitet klageResultat = klageRepository.hentEvtOpprettKlageResultat(behandling);
        KlageVurderingResultat.Builder klageVurderingResultatBuilder = new KlageVurderingResultat.Builder()
            .medBegrunnelse(adapter.getBegrunnelse())
            .medFritekstTilBrev(adapter.getFritekstTilBrev())
            .medKlageVurdering(klageVurdering)
            .medKlageVurdertAv(adapter.getErNfpAksjonspunkt() ? KlageVurdertAv.NFP : KlageVurdertAv.NK)
            .medKlageResultat(klageResultat);

        Optional<String> klageMedholdÅrsak = adapter.getKlageMedholdArsakKode();
        klageMedholdÅrsak.ifPresent(medholdÅrsak -> klageVurderingResultatBuilder
            .medKlageMedholdÅrsak(KlageMedholdÅrsak.fraKode(medholdÅrsak)));

        Optional<String> klageVurderingOmgjør = adapter.getKlageVurderingOmgjoer();
        klageVurderingOmgjør.ifPresent(vurdeingOmgjør -> klageVurderingResultatBuilder
            .medKlageVurderingOmgjør(KlageVurderingOmgjør.fraKode(vurdeingOmgjør)));

        klageVurderingResultatBuilder.medGodkjentAvMedunderskriver(erGodkjentAvMedunderskriver(behandling, klageVurderingResultatBuilder.build()));
        klageRepository.lagreVurderingsResultat(behandling, klageVurderingResultatBuilder);
    }

    private boolean erGodkjentAvMedunderskriver(Behandling behandling, KlageVurderingResultat klageVurderingResultat) {
        Optional<KlageVurderingResultat> gammeltKlageVurderingResultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
        return gammeltKlageVurderingResultat.isPresent() && gammeltKlageVurderingResultat.get().isGodkjentAvMedunderskriver()
            && gammeltKlageVurderingResultat.get().getKlageVurdering().equals(klageVurderingResultat.getKlageVurdering())
            && gammeltKlageVurderingResultat.get().getKlageMedholdÅrsak().equals(klageVurderingResultat.getKlageMedholdÅrsak())
            && gammeltKlageVurderingResultat.get().getKlageVurderingOmgjør().equals(klageVurderingResultat.getKlageVurderingOmgjør());
    }

    private void settBehandlingResultatTypeBasertPaaUtfall(Behandling behandling, KlageVurderingAdapter adapter) {
        KlageVurdering klageVurdering = KlageVurdering.fraKode(adapter.getKlageVurderingKode());
        if (adapter.getErNfpAksjonspunkt() && klageVurdering.equals(KlageVurdering.STADFESTE_YTELSESVEDTAK)
            && !Fagsystem.INFOTRYGD.equals(behandling.getMigrertKilde())) {

            BestillBrevDto bestillBrevDto = new BestillBrevDto(behandling.getId(), DokumentMalType.KLAGE_OVERSENDT_KLAGEINSTANS);
            dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER, false);
            oppdaterBehandlingMedNyFrist(behandling);
        }
        if (behandling.getBehandlingsresultat() == null) {
            Behandlingsresultat.opprettFor(behandling);
        }
        KlageResultatEntitet klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling);

        boolean erPåklagdEksternBehandling = false;
        if(klageResultatEntitet.getPåKlagdBehandling().isEmpty() && klageResultatEntitet.getPåKlagdEksternBehandling().isPresent()){
            erPåklagdEksternBehandling = true;
        }
        Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
            .medBehandlingResultatType(BehandlingResultatType.tolkBehandlingResultatType(klageVurdering, erPåklagdEksternBehandling));
    }

    private void oppdaterBehandlingMedNyFrist(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingstidFrist(LocalDate.now().plusWeeks(14));
        behandlingRepository.lagre(behandling, lås);
    }
}
