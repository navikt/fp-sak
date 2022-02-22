package no.nav.foreldrepenger.domene.mappers.endringutleder_fra_entitet;

import java.util.Optional;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.oppdateringresultat.RefusjonoverstyringEndring;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;

public final class UtledEndringForRefusjonOverstyring {

    private UtledEndringForRefusjonOverstyring() {
        // skjul
    }

    public static Optional<RefusjonoverstyringEndring> utled(BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlagDto,
                                                   Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                                                   AksjonspunktKode dto) {
        if (dto instanceof VurderRefusjonBeregningsgrunnlagDto) {

            var refusjonOverstyringer = beregningsgrunnlagGrunnlagDto.getRefusjonOverstyringer()
                .orElseThrow(() -> new IllegalArgumentException("Skal ha refusjonoverstyringer her"));
            var forrigeRefusjonOverstyringer = forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getRefusjonOverstyringer);
            return Optional.of(UtledEndringIRefusjonsperiode.utledRefusjonoverstyringEndring(refusjonOverstyringer,
                beregningsgrunnlagGrunnlagDto.getBeregningsgrunnlag().orElseThrow(), forrigeRefusjonOverstyringer,
                forrigeGrunnlag.stream().flatMap(gr -> gr.getBeregningsgrunnlag().stream()).findFirst()));
        }
        return Optional.empty();
    }
}
