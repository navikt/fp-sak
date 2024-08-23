package no.nav.foreldrepenger.domene.aksjonspunkt;

import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.AvklarAktiviteterHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FaktaOmBeregningHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FaktaOmFordelingHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBGTidsbegrensetArbeidsforholdHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBeregningsgrunnlagATFLHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBeregningsgrunnlagSNNyIArbeidslivetHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.VurderVarigEndringEllerNyoppstartetSNHåndteringDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.domene.rest.dto.AvklarteAktiviteterDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;

public class KalkulusAksjonspunktMapper {

    private KalkulusAksjonspunktMapper() {
        // Hindrer instansiering
    }


    public static HåndterBeregningDto mapAksjonspunktTilKalkulusDto(BekreftetAksjonspunktDto dto) {
        return mapSpesifikkDto(dto);
    }

    private static HåndterBeregningDto mapSpesifikkDto(BekreftetAksjonspunktDto dto) {
        if (dto instanceof AvklarteAktiviteterDto avklarteAktiviteterDto) {
            return new AvklarAktiviteterHåndteringDto(OppdatererDtoMapper.mapAvklarteAktiviteterDto(avklarteAktiviteterDto));
        }
        if (dto instanceof VurderFaktaOmBeregningDto faktaOmBeregningDto) {
            return new FaktaOmBeregningHåndteringDto(OppdatererDtoMapper.mapTilFaktaOmBeregningLagreDto(faktaOmBeregningDto.getFakta()));
        }
        if (dto instanceof FastsettBeregningsgrunnlagATFLDto fastsettBeregningsgrunnlagATFLDto) {
            return new FastsettBeregningsgrunnlagATFLHåndteringDto(OppdatererDtoMapper.mapTilInntektPrAndelListe(fastsettBeregningsgrunnlagATFLDto.getInntektPrAndelList()), fastsettBeregningsgrunnlagATFLDto.getInntektFrilanser(), null);
        }
        if (dto instanceof FastsettBGTidsbegrensetArbeidsforholdDto fastsettBGTidsbegrensetArbeidsforholdDto) {
            return new FastsettBGTidsbegrensetArbeidsforholdHåndteringDto(OppdatererDtoMapper.mapFastsettBGTidsbegrensetArbeidsforholdDto(fastsettBGTidsbegrensetArbeidsforholdDto));
        }
        if (dto instanceof VurderVarigEndringEllerNyoppstartetSNDto vurderVarigEndringEllerNyoppstartetSNDto) {
            return new VurderVarigEndringEllerNyoppstartetSNHåndteringDto(OppdatererDtoMapper.mapVurderVarigEndringEllerNyoppstartetDto(vurderVarigEndringEllerNyoppstartetSNDto));
        }
        if (dto instanceof FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto fastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto) {
            return new FastsettBeregningsgrunnlagSNNyIArbeidslivetHåndteringDto(OppdatererDtoMapper.mapFastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(fastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto));
        }
        if (dto instanceof FordelBeregningsgrunnlagDto fordelBeregningsgrunnlagDto) {
            return new FaktaOmFordelingHåndteringDto(OppdatererDtoMapper.mapFordelBeregningsgrunnlagDto(fordelBeregningsgrunnlagDto));
        }
        if (dto instanceof VurderRefusjonBeregningsgrunnlagDto) {
            return OppdatererDtoMapper.mapVurderRefusjonBeregningsgrunnlag((VurderRefusjonBeregningsgrunnlagDto) dto);
        }
        throw new IllegalStateException("Aksjonspunkt er ikke mappet i kalkulus");
    }
}
