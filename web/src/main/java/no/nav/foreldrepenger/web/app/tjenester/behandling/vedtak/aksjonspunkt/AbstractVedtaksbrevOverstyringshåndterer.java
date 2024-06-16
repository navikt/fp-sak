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
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

public abstract class AbstractVedtaksbrevOverstyringshåndterer {

    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private VedtakTjeneste vedtakTjeneste;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag;

    AbstractVedtaksbrevOverstyringshåndterer(BehandlingRepository behandlingRepository,
                                             BehandlingsresultatRepository behandlingsresultatRepository,
                                             HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                             VedtakTjeneste vedtakTjeneste,
                                             BehandlingDokumentRepository behandlingDokumentRepository,
                                             OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.vedtakTjeneste = vedtakTjeneste;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.behandlingRepository = behandlingRepository;
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
    }

    AbstractVedtaksbrevOverstyringshåndterer() {
        //CDI
    }

    OppdateringResultat håndter(VedtaksbrevOverstyringDto dto, AksjonspunktOppdaterParameter param, boolean toTrinn) {
        var begrunnelse = dto.getBegrunnelse();
        var behandling = getBehandling(param.getBehandlingId());

        oppdaterBegrunnelse(behandling, begrunnelse);
        var builder = OppdateringResultat.utenTransisjon()
            .medTotrinnHvis(dto.isSkalBrukeOverstyrendeFritekstBrev());
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            oppdaterFritekstVedtaksbrev(dto, param);
        } else {
            fjernFritekstBrevHvisEksisterer(param.getBehandlingId());
        }
        opprettHistorikkinnslag(behandling, toTrinn);
        if (toTrinn) {
            opprettAksjonspunktForFatterVedtak(behandling, builder);
            opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);
            behandling.setToTrinnsBehandling();
        } else {
            behandling.nullstillToTrinnsBehandling();
        }
        return builder.build();
    }

    private void oppdaterFritekstVedtaksbrev(VedtaksbrevOverstyringDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = getBehandling(param.getBehandlingId());
        settFritekstBrev(behandling, dto.getOverskrift(), dto.getFritekstBrev());
    }

    private Behandling getBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
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

    private void fjernFritekstBrevHvisEksisterer(long behandlingId) {
        var eksisterendeBehandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandlingId);
        if (eksisterendeBehandlingDokument.isPresent() && erFritekst(eksisterendeBehandlingDokument.get())) {
            var behandlingDokument = getBehandlingDokumentBuilder(eksisterendeBehandlingDokument)
                    .medBehandling(behandlingId)
                    .medOverstyrtBrevOverskrift(null)
                    .medOverstyrtBrevFritekst(null)
                    .build();
            behandlingDokumentRepository.lagreOgFlush(behandlingDokument);
            var behandling = getBehandling(behandlingId);
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

    private void opprettAksjonspunktForFatterVedtak(Behandling behandling, OppdateringResultat.Builder builder) {
        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return; // vedtak for innsynsbehanding fattes automatisk
        }

        builder.medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.FATTER_VEDTAK, AksjonspunktStatus.OPPRETTET);
    }

    private void oppdaterBegrunnelse(Behandling behandling, String begrunnelse) {
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());

        behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).ifPresent(behandlingsresultat -> {
            if (BehandlingType.KLAGE.equals(behandling.getType()) || behandlingsresultat.isBehandlingsresultatAvslåttOrOpphørt()
                || begrunnelse != null || skalNullstilleFritekstfelt(behandling, behandlingsresultat, behandlingDokument)) {

                var behandlingDokumentBuilder = getBehandlingDokumentBuilder(behandlingDokument);
                behandlingDokumentBuilder.medBehandling(behandling.getId());
                behandlingDokumentBuilder.medVedtakFritekst(begrunnelse);
                behandlingDokumentRepository.lagreOgFlush(behandlingDokumentBuilder.build());
            }
        });
    }

    private boolean skalNullstilleFritekstfelt(Behandling behandling, Behandlingsresultat behandlingsresultat,
                                               Optional<BehandlingDokumentEntitet> behandlingDokument) {
        return !BehandlingType.KLAGE.equals(behandling.getType()) && !behandlingsresultat.isBehandlingsresultatAvslåttOrOpphørt()
            && behandlingDokument.isPresent() && behandlingDokument.get().getVedtakFritekst() != null;
    }

    private void opprettHistorikkinnslag(Behandling behandling, boolean toTrinn) {
        var vedtakResultatType = vedtakTjeneste.utledVedtakResultatType(behandling);
        var historikkInnslagType = utledHistorikkInnslag(behandling, toTrinn);

        var tekstBuilder = new HistorikkInnslagTekstBuilder()
            .medResultat(vedtakResultatType)
            .medSkjermlenke(SkjermlenkeType.VEDTAK)
            .medHendelse(historikkInnslagType);

        var innslag = new Historikkinnslag();
        innslag.setType(historikkInnslagType);
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setBehandlingId(behandling.getId());
        tekstBuilder.build(innslag);
        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }

    private HistorikkinnslagType utledHistorikkInnslag(Behandling behandling, boolean toTrinn) {
        if (BehandlingType.INNSYN.equals(behandling.getType()) || !toTrinn) {
            return HistorikkinnslagType.FORSLAG_VEDTAK_UTEN_TOTRINN;
        }
        return HistorikkinnslagType.FORSLAG_VEDTAK;
    }

    private BehandlingDokumentEntitet.Builder getBehandlingDokumentBuilder(long behandlingId) {
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandlingId);
        return getBehandlingDokumentBuilder(behandlingDokument);
    }

    private BehandlingDokumentEntitet.Builder getBehandlingDokumentBuilder(Optional<BehandlingDokumentEntitet> behandlingDokument) {
        return behandlingDokument.map(BehandlingDokumentEntitet.Builder::fraEksisterende).orElseGet(BehandlingDokumentEntitet.Builder::ny);
    }
}
