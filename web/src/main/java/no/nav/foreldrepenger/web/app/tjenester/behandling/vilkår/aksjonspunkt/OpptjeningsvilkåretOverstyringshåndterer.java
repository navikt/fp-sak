package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringOpptjeningsvilkåretDto;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringOpptjeningsvilkåretDto.class, adapter = Overstyringshåndterer.class)
public class OpptjeningsvilkåretOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringOpptjeningsvilkåretDto> {

    private OpptjeningRepository opptjeningRepository;

    OpptjeningsvilkåretOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningsvilkåretOverstyringshåndterer(OpptjeningRepository opptjeningRepository,
                                                    InngangsvilkårTjeneste inngangsvilkårTjeneste,
                                                    Historikkinnslag2Repository historikkinnslag2Repository) {
        super(AksjonspunktDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET,
            VilkårType.OPPTJENINGSVILKÅRET,
            inngangsvilkårTjeneste, historikkinnslag2Repository);
        this.opptjeningRepository = opptjeningRepository;
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringOpptjeningsvilkåretDto dto) {
        lagHistorikkInnslagForOverstyrtVilkår(behandling, dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_OPPTJENING);
    }

    @Override
    protected void precondition(Behandling behandling, OverstyringOpptjeningsvilkåretDto dto) {
        if (dto.getErVilkarOk()) {
            var ant = opptjeningRepository.finnOpptjening(behandling.getId()).map(Opptjening::getOpptjeningAktivitet).orElse(List.of()).stream()
                .filter(oa -> !oa.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD))
                .count();
            if (ant > 0) {
                return;
            }
            throw new FunksjonellException( "FP-093923",
                "Kan ikke overstyre vilkår. Det må være minst en aktivitet for at opptjeningsvilkåret skal kunne overstyres.",
                "Sett på vent til det er mulig og manuelt legge inn aktiviteter ved overstyring.");
        }
    }
}
