package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Optional;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

public abstract class AbstractVedtaksbrevOverstyringshåndterer {
    protected HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    BehandlingsresultatRepository behandlingsresultatRepository;
    protected OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag;
    private VedtakTjeneste vedtakTjeneste;
    private BehandlingDokumentRepository behandlingDokumentRepository;

    AbstractVedtaksbrevOverstyringshåndterer() {
        // for CDI proxy
    }

    AbstractVedtaksbrevOverstyringshåndterer(BehandlingRepositoryProvider repositoryProvider,
                                             HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                             OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                             VedtakTjeneste vedtakTjeneste) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
        this.vedtakTjeneste = vedtakTjeneste;
        this.behandlingDokumentRepository = null;
    }

    AbstractVedtaksbrevOverstyringshåndterer(BehandlingRepositoryProvider repositoryProvider,
                                             HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                             OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                             VedtakTjeneste vedtakTjeneste,
                                             BehandlingDokumentRepository behandlingDokumentRepository) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
        this.vedtakTjeneste = vedtakTjeneste;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
    }

    void oppdaterVedtaksbrev(VedtaksbrevOverstyringDto dto, AksjonspunktOppdaterParameter param, OppdateringResultat.Builder builder) {
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            Behandling behandling = param.getBehandling();
            settFritekstBrev(behandling, dto.getOverskrift(), dto.getFritekstBrev());
            opprettToTrinnsKontrollpunktForFritekstBrev(dto, behandling, builder);
            opprettAksjonspunktForFatterVedtak(behandling, builder);
            opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);
            opprettHistorikkinnslag(behandling);
        }
    }

    private void settFritekstBrev(Behandling behandling, String overskrift, String fritekst) {
        behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).ifPresent(behandlingsresultat -> {
            Optional<BehandlingDokumentEntitet> behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
            BehandlingDokumentEntitet.Builder behandlingDokumentBuilder = getBehandlingDokumentBuilder(behandlingDokument);
            behandlingDokumentRepository.lagreOgFlush(behandlingDokumentBuilder
                .medBehandling(behandling.getId())
                .medOverstyrtBrevOverskrift(overskrift)
                .medOverstyrtBrevFritekst(fritekst)
                .build());
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                .medOverskrift(overskrift)
                .medFritekstbrev(fritekst)
                .medVedtaksbrev(Vedtaksbrev.FRITEKST)
                .buildFor(behandling);
        });
    }

    private void opprettToTrinnsKontrollpunktForFritekstBrev(BekreftetAksjonspunktDto dto, Behandling behandling, OppdateringResultat.Builder builder) {
        if (!behandling.isToTrinnsBehandling()) {
            behandling.setToTrinnsBehandling();
        }
        builder.medTotrinn();
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());
        if (!AksjonspunktDefinisjon.FORESLÅ_VEDTAK.equals(aksjonspunktDefinisjon)) {
            ekskluderOrginaltAksjonspunktFraTotrinnsVurdering(dto, behandling, builder);
            registrerNyttKontrollpunktIAksjonspunktRepo(behandling, builder);
        }
    }

    void opprettAksjonspunktForFatterVedtak(Behandling behandling, OppdateringResultat.Builder builder) {
        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return; //vedtak for innsynsbehanding fattes automatisk
        }

        builder.medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.FATTER_VEDTAK, AksjonspunktStatus.OPPRETTET);
    }

    private void ekskluderOrginaltAksjonspunktFraTotrinnsVurdering(BekreftetAksjonspunktDto dto, Behandling behandling, OppdateringResultat.Builder builder) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());
        behandling.getÅpentAksjonspunktMedDefinisjonOptional(aksjonspunktDefinisjon)
            .ifPresent(ap -> builder.medAvbruttAksjonspunkt());
    }

    private void registrerNyttKontrollpunktIAksjonspunktRepo(Behandling behandling, OppdateringResultat.Builder builder) {
        AksjonspunktDefinisjon foreslaVedtak = AksjonspunktDefinisjon.FORESLÅ_VEDTAK;
        AksjonspunktStatus target = behandling.getAksjonspunktMedDefinisjonOptional(foreslaVedtak)
            .map(ap -> AksjonspunktStatus.AVBRUTT.equals(ap.getStatus()) ? AksjonspunktStatus.OPPRETTET : ap.getStatus()).orElse(AksjonspunktStatus.UTFØRT);
        builder.medEkstraAksjonspunktResultat(foreslaVedtak, target);
    }

    void opprettHistorikkinnslag(Behandling behandling) {
        VedtakResultatType vedtakResultatType = vedtakTjeneste.utledVedtakResultatType(behandling);

        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder()
            .medResultat(vedtakResultatType)
            .medSkjermlenke(SkjermlenkeType.VEDTAK)
            .medHendelse(BehandlingType.INNSYN.equals(behandling.getType()) ? HistorikkinnslagType.FORSLAG_VEDTAK_UTEN_TOTRINN : HistorikkinnslagType.FORSLAG_VEDTAK);

        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setType(BehandlingType.INNSYN.equals(behandling.getType()) ? HistorikkinnslagType.FORSLAG_VEDTAK_UTEN_TOTRINN : HistorikkinnslagType.FORSLAG_VEDTAK);
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setBehandlingId(behandling.getId());
        tekstBuilder.build(innslag);
        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }

    BehandlingDokumentEntitet.Builder getBehandlingDokumentBuilder(Optional<BehandlingDokumentEntitet> behandlingDokument) {
        return behandlingDokument.map(BehandlingDokumentEntitet.Builder::fraEksisterende).orElseGet(BehandlingDokumentEntitet.Builder::ny);
    }
}
