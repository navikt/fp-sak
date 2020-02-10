package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.dokumentbestiller.klient.FormidlingRestKlient;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BehandlingUuidDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.TekstFraSaksbehandlerDto;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer extends AbstractVedtaksbrevOverstyringshåndterer implements AksjonspunktOppdaterer<ForeslaVedtakAksjonspunktDto> {


    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                               HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                               OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                               VedtakTjeneste vedtakTjeneste,
                                               FormidlingRestKlient formidlingRestKlient,
                                               Unleash unleash) {
        super(repositoryProvider, historikkApplikasjonTjeneste, opprettToTrinnsgrunnlag, vedtakTjeneste, formidlingRestKlient, unleash);
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        String begrunnelse = dto.getBegrunnelse();
        Behandling behandling = param.getBehandling();
        oppdaterBegrunnelse(behandling, begrunnelse);

        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            super.oppdaterVedtaksbrev(dto, param, builder);
        } else {
            opprettAksjonspunktForFatterVedtak(behandling, builder);
            opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);
            opprettHistorikkinnslag(behandling);
        }
        return builder.build();
    }

    private void oppdaterBegrunnelse(Behandling behandling, String begrunnelse) {
        behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).ifPresent(behandlingsresultat -> {
            if ((BehandlingType.KLAGE.equals(behandling.getType()) || behandlingsresultat.isBehandlingsresultatAvslåttOrOpphørt() || begrunnelse != null)
                || skalNullstilleFritekstfelt(behandling, behandlingsresultat)) {
                if (unleash.isEnabled(FPSAK_LAGRE_FRITEKST_INN_FORMIDLING)) {
                    Optional<TekstFraSaksbehandlerDto> tekstFraSaksbehandlerDtoOptional = formidlingRestKlient.hentTekstFraSaksbehandler(new BehandlingUuidDto(behandling.getUuid()));
                    if (tekstFraSaksbehandlerDtoOptional.isPresent()
                        && Objects.nonNull(tekstFraSaksbehandlerDtoOptional.get().getAvklarFritekst())) {
                        final TekstFraSaksbehandlerDto tekstFraSaksbehandlerDto = tekstFraSaksbehandlerDtoOptional.get();
                        tekstFraSaksbehandlerDto.setAvklarFritekst(begrunnelse);
                        formidlingRestKlient.lagreTekstFraSaksbehandler(tekstFraSaksbehandlerDto);
                    }
                } else {
                    behandlingsresultat.setAvslagarsakFritekst(begrunnelse);
                }
            }
        });
        behandling.setAnsvarligSaksbehandler(getCurrentUserId());
    }

    private boolean skalNullstilleFritekstfelt(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        return !BehandlingType.KLAGE.equals(behandling.getType()) && !behandlingsresultat.isBehandlingsresultatAvslåttOrOpphørt()
            && behandlingsresultat.getAvslagarsakFritekst() != null;
    }

    protected String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }
}
