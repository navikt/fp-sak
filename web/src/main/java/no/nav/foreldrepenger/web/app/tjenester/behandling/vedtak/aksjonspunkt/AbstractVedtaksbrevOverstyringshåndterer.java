package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Optional;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
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
    private VedtakTjeneste vedtakTjeneste;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private BehandlingRepository behandlingRepository;

    AbstractVedtaksbrevOverstyringshåndterer() {
        // for CDI proxy
    }

    AbstractVedtaksbrevOverstyringshåndterer(BehandlingRepositoryProvider repositoryProvider,
                                             HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                             VedtakTjeneste vedtakTjeneste,
                                             BehandlingDokumentRepository behandlingDokumentRepository) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.vedtakTjeneste = vedtakTjeneste;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    void oppdaterFritekstVedtaksbrev(VedtaksbrevOverstyringDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        settFritekstBrev(behandling, dto.getOverskrift(), dto.getFritekstBrev());
        opprettHistorikkinnslag(behandling);
    }

    private void settFritekstBrev(Behandling behandling, String overskrift, String fritekst) {
        var behandlingId = behandling.getId();
        var behandlingDokumentBuilder = getBehandlingDokumentBuilder(behandlingId);
        behandlingDokumentRepository.lagreOgFlush(behandlingDokumentBuilder
            .medBehandling(behandlingId)
            .medOverstyrtBrevOverskrift(overskrift)
            .medOverstyrtBrevFritekst(fritekst)
            .build());
        var behandlingsresultat = getBehandlingsresultatBuilder(behandlingId)
            .medVedtaksbrev(Vedtaksbrev.FRITEKST)
            .buildFor(behandling);
        behandlingsresultatRepository.lagre(behandlingId, behandlingsresultat);
    }

    void fjernFritekstBrevHvisEksisterer(long behandlingId) {
        var eksisterendeBehandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandlingId);
        if (eksisterendeBehandlingDokument.isPresent() && erFritekst(eksisterendeBehandlingDokument.get())) {
            var behandlingDokument = getBehandlingDokumentBuilder(eksisterendeBehandlingDokument)
                .medBehandling(behandlingId)
                .medOverstyrtBrevOverskrift(null)
                .medOverstyrtBrevFritekst(null)
                .build();
            behandlingDokumentRepository.lagreOgFlush(behandlingDokument);
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            var behandlingResultat = getBehandlingsresultatBuilder(behandlingId)
                .medVedtaksbrev(Vedtaksbrev.AUTOMATISK)
                .buildFor(behandling);
            behandlingsresultatRepository.lagre(behandlingId, behandlingResultat);
        }
    }

    private boolean erFritekst(BehandlingDokumentEntitet dok) {
        return dok.getOverstyrtBrevFritekst() != null;
    }

    private Behandlingsresultat.Builder getBehandlingsresultatBuilder(long behandlingId) {
        var eksisterendeBehandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        return Behandlingsresultat.builderEndreEksisterende(eksisterendeBehandlingsresultat);
    }

    void opprettAksjonspunktForFatterVedtak(Behandling behandling, OppdateringResultat.Builder builder) {
        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return; //vedtak for innsynsbehanding fattes automatisk
        }

        builder.medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.FATTER_VEDTAK, AksjonspunktStatus.OPPRETTET);
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

    BehandlingDokumentEntitet.Builder getBehandlingDokumentBuilder(long behandlingId) {
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandlingId);
        return getBehandlingDokumentBuilder(behandlingDokument);
    }

    BehandlingDokumentEntitet.Builder getBehandlingDokumentBuilder(Optional<BehandlingDokumentEntitet> behandlingDokument) {
        return behandlingDokument.map(BehandlingDokumentEntitet.Builder::fraEksisterende).orElseGet(BehandlingDokumentEntitet.Builder::ny);
    }
}
