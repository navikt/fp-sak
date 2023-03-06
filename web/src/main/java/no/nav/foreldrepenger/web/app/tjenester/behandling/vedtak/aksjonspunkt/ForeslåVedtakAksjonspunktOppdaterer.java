package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer extends AbstractVedtaksbrevOverstyringshåndterer
        implements AksjonspunktOppdaterer<ForeslaVedtakAksjonspunktDto> {

    private BehandlingDokumentRepository behandlingDokumentRepository;
    private OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag;

    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(BehandlingRepository behandlingRepository,
                                               BehandlingsresultatRepository behandlingsresultatRepository,
                                               HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                               OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                               VedtakTjeneste vedtakTjeneste,
                                               BehandlingDokumentRepository behandlingDokumentRepository) {
        super(behandlingRepository, behandlingsresultatRepository, historikkApplikasjonTjeneste, vedtakTjeneste, behandlingDokumentRepository);
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var begrunnelse = dto.getBegrunnelse();
        var behandling = getBehandling(param.getBehandlingId());

        oppdaterBegrunnelse(behandling, begrunnelse);
        var builder = OppdateringResultat.utenTransisjon();
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            oppdaterFritekstVedtaksbrev(dto, param);
        } else {
            fjernFritekstBrevHvisEksisterer(param.getBehandlingId());
        }
        opprettAksjonspunktForFatterVedtak(behandling, builder);
        opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);
        opprettHistorikkinnslag(behandling);
        return builder.medTotrinnHvis(dto.isSkalBrukeOverstyrendeFritekstBrev()).build();
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
        behandling.setAnsvarligSaksbehandler(getCurrentUserId());
    }

    private boolean skalNullstilleFritekstfelt(Behandling behandling, Behandlingsresultat behandlingsresultat,
            Optional<BehandlingDokumentEntitet> behandlingDokument) {
        return !BehandlingType.KLAGE.equals(behandling.getType()) && !behandlingsresultat.isBehandlingsresultatAvslåttOrOpphørt()
                && behandlingDokument.isPresent() && behandlingDokument.get().getVedtakFritekst() != null;
    }

    protected String getCurrentUserId() {
        return KontekstHolder.getKontekst().getUid();
    }
}
