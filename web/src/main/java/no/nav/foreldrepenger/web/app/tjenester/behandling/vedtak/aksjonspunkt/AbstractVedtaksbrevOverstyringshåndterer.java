package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.vedtak.exception.TekniskException;

public abstract class AbstractVedtaksbrevOverstyringshåndterer {

    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private VedtakTjeneste vedtakTjeneste;

    AbstractVedtaksbrevOverstyringshåndterer(BehandlingRepository behandlingRepository,
                                             BehandlingsresultatRepository behandlingsresultatRepository,
                                             HistorikkinnslagRepository historikkinnslagRepository,
                                             VedtakTjeneste vedtakTjeneste,
                                             BehandlingDokumentRepository behandlingDokumentRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.vedtakTjeneste = vedtakTjeneste;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.behandlingRepository = behandlingRepository;
    }

    AbstractVedtaksbrevOverstyringshåndterer() {
        //CDI
    }

    OppdateringResultat håndter(VedtaksbrevOverstyringDto dto, AksjonspunktOppdaterParameter param, boolean toTrinn) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterGrunnlagForVedtaksbrev(dto, behandling);
        opprettHistorikkinnslag(param.getRef(), behandling, toTrinn, dto.isSkalBrukeOverstyrendeFritekstBrev());

        var builder = OppdateringResultat.utenTransisjon()
            .medTotrinnHvis(dto.isSkalBrukeOverstyrendeFritekstBrev());
        if (toTrinn) {
            opprettAksjonspunktForFatterVedtak(behandling, builder);
            behandling.setToTrinnsBehandling();
        } else {
            behandling.nullstillToTrinnsBehandling();
        }
        return builder.build();
    }

    private void oppdaterGrunnlagForVedtaksbrev(VedtaksbrevOverstyringDto dto, Behandling behandling) {
        var behandlingId = behandling.getId();
        var utfyllendeTekst = dto.getBegrunnelse();
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            fjernUtfyllendeTekstForAutomatiskVedtaksbrevHvisSatt(behandlingId);
            verifiserBehandlingDokumentInneholderRedigertBrev(behandlingId);
            var behandlingsresultat = getBehandlingsresultatBuilder(behandlingId)
                .medVedtaksbrev(Vedtaksbrev.FRITEKST)
                .buildFor(behandling); // .buildFor oppdaterer også ES legacy BR hvis det foreligger.
            behandlingsresultatRepository.lagre(behandlingId, behandlingsresultat);
        } else {
            // Automatisk vedtaksbrev med mulig utfyllende fritekst (satt av begrunnelse)
            fjernEksisterendeRedigertEllerOverstyrBrevHvisEksisterer(behandling);
            if (utfyllendeTekst != null) {
                var behandlingDokument = getBehandlingDokumentBuilder(behandlingId)
                    .medBehandling(behandlingId)
                    .medUtfyllendeTekstAutomatiskVedtaksbrev(utfyllendeTekst)
                    .build();
                behandlingDokumentRepository.lagreOgFlush(behandlingDokument);
            } else if (skalFjerneUtfyllendeTekstForAutomatiskVedtaksbrev(behandling)) {
                fjernUtfyllendeTekstForAutomatiskVedtaksbrevHvisSatt(behandlingId);
            }
        }
    }

    private void fjernUtfyllendeTekstForAutomatiskVedtaksbrevHvisSatt(Long behandlingId) {
        var behandlingDokumentEntitetOpt = behandlingDokumentRepository.hentHvisEksisterer(behandlingId);
        if (behandlingDokumentEntitetOpt.isPresent() && behandlingDokumentEntitetOpt.get().getVedtakFritekst() != null) {
            var behandlingDokument = BehandlingDokumentEntitet.Builder.fraEksisterende(behandlingDokumentEntitetOpt.get())
                .medBehandling(behandlingId)
                .medUtfyllendeTekstAutomatiskVedtaksbrev(null)
                .build();
            behandlingDokumentRepository.lagreOgFlush(behandlingDokument);
        }
    }

    private void verifiserBehandlingDokumentInneholderRedigertBrev(Long behandlingId) {
        var behandlingDokumentEntitet = behandlingDokumentRepository.hentHvisEksisterer(behandlingId);
        if (behandlingDokumentEntitet.isEmpty() || behandlingDokumentEntitet.get().getOverstyrtBrevFritekstHtml() == null) {
            throw new TekniskException("FP-666916", "Foreslå vedtaksteget har sendt at brev skal overstyres til beslutter men det foreligger ingen overstyring!");
        }
    }

    private void fjernEksisterendeRedigertEllerOverstyrBrevHvisEksisterer(Behandling behandling) {
        var behandlingId = behandling.getId();
        var eksisterendeBehandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandlingId);
        if (eksisterendeBehandlingDokument.isPresent() && erFritekst(eksisterendeBehandlingDokument.get())) {
            var behandlingDokument = BehandlingDokumentEntitet.Builder.fraEksisterende(eksisterendeBehandlingDokument.get())
                .medBehandling(behandlingId)
                .medOverstyrtBrevOverskrift(null)
                .medOverstyrtBrevFritekst(null)
                .medOverstyrtBrevFritekstHtml(null)
                .build();
            behandlingDokumentRepository.lagreOgFlush(behandlingDokument);
        }

        var behandlingsresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        if (behandlingsresultatOpt.isPresent() && Vedtaksbrev.FRITEKST.equals(behandlingsresultatOpt.get().getVedtaksbrev())) {
            var behandlingResultat = Behandlingsresultat.builderEndreEksisterende(behandlingsresultatOpt.get())
                .medVedtaksbrev(Vedtaksbrev.AUTOMATISK)
                .buildFor(behandling); // .buildFor oppdaterer også ES legacy BR hvis det foreligger.
            behandlingsresultatRepository.lagre(behandlingId, behandlingResultat);
        }
    }

    private static boolean erFritekst(BehandlingDokumentEntitet dok) {
        return dok.getOverstyrtBrevFritekst() != null || dok.getOverstyrtBrevFritekstHtml() != null;
    }

    private boolean skalFjerneUtfyllendeTekstForAutomatiskVedtaksbrev(Behandling behandling) {
        var behandlingsresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        var behandlingDokumentEntitetOpt = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        if (behandlingsresultatOpt.isEmpty() || behandlingDokumentEntitetOpt.isEmpty()) {
            return false;
        }

        return !BehandlingType.KLAGE.equals(behandling.getType()) &&
            !behandlingsresultatOpt.get().isBehandlingsresultatAvslåttOrOpphørt() &&
            behandlingDokumentEntitetOpt.get().getVedtakFritekst() != null;
    }

    private Behandlingsresultat.Builder getBehandlingsresultatBuilder(long behandlingId) {
        var eksisterendeBehandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        return Behandlingsresultat.builderEndreEksisterende(eksisterendeBehandlingsresultat);
    }

    private void opprettAksjonspunktForFatterVedtak(Behandling behandling, OppdateringResultat.Builder builder) {
        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return; // vedtak for innsynsbehanding fattes automatisk
        }

        builder.medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.FATTER_VEDTAK, AksjonspunktStatus.OPPRETTET);
    }

    private BehandlingDokumentEntitet.Builder getBehandlingDokumentBuilder(long behandlingId) {
        return behandlingDokumentRepository.hentHvisEksisterer(behandlingId)
            .map(BehandlingDokumentEntitet.Builder::fraEksisterende)
            .orElseGet(() -> BehandlingDokumentEntitet.Builder.ny().medBehandling(behandlingId));
    }

    private void opprettHistorikkinnslag(BehandlingReferanse ref, Behandling behandling, boolean toTrinn, boolean erBrevetOverstyrt) {
        var hendelseTekst = BehandlingType.INNSYN.equals(behandling.getType()) || !toTrinn
            ? "Vedtak er foreslått"
            : "Vedtak er foreslått og sendt til beslutter";
        var vedtakResultatType = vedtakTjeneste.utledVedtakResultatType(behandling);
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.VEDTAK)
            .addLinje(String.format("%s: %s", hendelseTekst, vedtakResultatType.getNavn()))
            .addLinje(erBrevetOverstyrt ? "Vedtaksbrevet er overstyrt" : null)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
