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
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer extends AbstractVedtaksbrevOverstyringshåndterer implements AksjonspunktOppdaterer<ForeslaVedtakAksjonspunktDto> {

    private BehandlingDokumentRepository behandlingDokumentRepository;

    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                               HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                               OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                               VedtakTjeneste vedtakTjeneste,
                                               BehandlingDokumentRepository behandlingDokumentRepository) {
        super(repositoryProvider, historikkApplikasjonTjeneste, opprettToTrinnsgrunnlag, vedtakTjeneste, behandlingDokumentRepository);
        this.behandlingDokumentRepository = behandlingDokumentRepository;
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        String begrunnelse = dto.getBegrunnelse();
        Behandling behandling = param.getBehandling();
        oppdaterBegrunnelse(behandling, begrunnelse);

        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            oppdaterFritekstVedtaksbrev(dto, param, builder);
        } else {
            opprettAksjonspunktForFatterVedtak(behandling, builder);
            opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);
            opprettHistorikkinnslag(behandling);
            fjernFritekstBrevHvisEksisterer(param.getBehandlingId());
        }
        return builder.build();
    }

    private void oppdaterBegrunnelse(Behandling behandling, String begrunnelse) {
        Optional<BehandlingDokumentEntitet> behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());

        behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).ifPresent(behandlingsresultat -> {
            if (BehandlingType.KLAGE.equals(behandling.getType()) || behandlingsresultat.isBehandlingsresultatAvslåttOrOpphørt()
                || begrunnelse != null || skalNullstilleFritekstfelt(behandling, behandlingsresultat, behandlingDokument)) {

                BehandlingDokumentEntitet.Builder behandlingDokumentBuilder = getBehandlingDokumentBuilder(behandlingDokument);
                behandlingDokumentBuilder.medBehandling(behandling.getId());
                behandlingDokumentBuilder.medVedtakFritekst(begrunnelse);
                behandlingDokumentRepository.lagreOgFlush(behandlingDokumentBuilder.build());
            }
        });
        behandling.setAnsvarligSaksbehandler(getCurrentUserId());
    }

    private boolean skalNullstilleFritekstfelt(Behandling behandling, Behandlingsresultat behandlingsresultat, Optional<BehandlingDokumentEntitet> behandlingDokument) {
        return !BehandlingType.KLAGE.equals(behandling.getType()) && !behandlingsresultat.isBehandlingsresultatAvslåttOrOpphørt()
            && behandlingDokument.isPresent() && behandlingDokument.get().getVedtakFritekst() != null;
    }

    protected String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }
}
