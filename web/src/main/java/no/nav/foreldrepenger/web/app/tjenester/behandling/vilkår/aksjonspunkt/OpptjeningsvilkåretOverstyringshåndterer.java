package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringOpptjeningsvilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringOpptjeningsvilkåretDto.class, adapter = Overstyringshåndterer.class)
public class OpptjeningsvilkåretOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringOpptjeningsvilkåretDto> {

    private OpptjeningRepository opptjeningRepository;

    OpptjeningsvilkåretOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningsvilkåretOverstyringshåndterer(OpptjeningRepository opptjeningRepository,
                                                    HistorikkTjenesteAdapter historikkAdapter,
                                                    InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET,
            VilkårType.OPPTJENINGSVILKÅRET,
            inngangsvilkårTjeneste);
        this.opptjeningRepository = opptjeningRepository;
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringOpptjeningsvilkåretDto dto) {
        lagHistorikkInnslagForOverstyrtVilkår(dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_OPPTJENING);
    }

    @Override
    protected void precondition(Behandling behandling, OverstyringOpptjeningsvilkåretDto dto) {
        if (dto.getErVilkarOk()) {
            final Optional<Opptjening> opptjening = opptjeningRepository.finnOpptjening(behandling.getId());
            if (opptjening.isPresent()) {
                final long antall = opptjening.get().getOpptjeningAktivitet().stream()
                    .filter(oa -> !oa.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD)).count();
                if (antall > 0) {
                    return;
                }
            }
            throw OverstyringFeil.FACTORY.opptjeningPreconditionFailed().toException();
        }
    }
}
