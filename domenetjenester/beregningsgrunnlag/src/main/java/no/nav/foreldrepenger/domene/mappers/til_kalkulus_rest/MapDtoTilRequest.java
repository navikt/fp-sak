package no.nav.foreldrepenger.domene.mappers.til_kalkulus_rest;

import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.AvklarAktiviteterHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FaktaOmBeregningHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FaktaOmFordelingHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBGTidsbegrensetArbeidsforholdHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBeregningsgrunnlagATFLHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBruttoBeregningsgrunnlagSNHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.VurderVarigEndringEllerNyoppstartetSNHåndteringDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.overstyring.OverstyrBeregningsgrunnlagHåndteringDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.domene.rest.dto.AvklarteAktiviteterDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsaktiviteterDto;
import no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;

public class MapDtoTilRequest {

    /**
     * Mapper aksjonspunktdto til håndteringdto i kalkulus.
     *
     * @param dto BekreftAksjonspunktDto
     * @return Dto for håndtering av aksjonspunk i Kalkulus
     */

    public static HåndterBeregningDto map(BekreftetAksjonspunktDto dto) {
        HåndterBeregningDto håndterBeregningDto = mapSpesifikkDto(dto);
        håndterBeregningDto.setBegrunnelse(dto.getBegrunnelse());
        return håndterBeregningDto;
    }

    public static HåndterBeregningDto mapSpesifikkDto(BekreftetAksjonspunktDto dto) {
        if (dto instanceof AvklarteAktiviteterDto avklarteAktiviteterDto) {
            return new AvklarAktiviteterHåndteringDto(OppdatererKontraktMapper.mapAvklarteAktiviteterDto(avklarteAktiviteterDto));
        }
        if (dto instanceof VurderFaktaOmBeregningDto faktaOmBeregningDto) {
            return new FaktaOmBeregningHåndteringDto(OppdatererKontraktMapper.mapTilFaktaOmBeregningLagreDto(faktaOmBeregningDto.getFakta()));
        }
        if (dto instanceof FastsettBeregningsgrunnlagATFLDto fastsettBeregningsgrunnlagATFLDto) {
            return new FastsettBeregningsgrunnlagATFLHåndteringDto(OppdatererKontraktMapper.mapTilInntektPrAndelListe(fastsettBeregningsgrunnlagATFLDto.getInntektPrAndelList()), fastsettBeregningsgrunnlagATFLDto.getInntektFrilanser(), null);
        }
        if (dto instanceof FastsettBGTidsbegrensetArbeidsforholdDto fastsettBGTidsbegrensetArbeidsforholdDto) {
            return new FastsettBGTidsbegrensetArbeidsforholdHåndteringDto(OppdatererKontraktMapper.mapFastsettBGTidsbegrensetArbeidsforholdDto(fastsettBGTidsbegrensetArbeidsforholdDto));
        }
        if (dto instanceof FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto fastsettBruttoBeregningsgrunnlagSNDto) {
            return new FastsettBruttoBeregningsgrunnlagSNHåndteringDto(OppdatererKontraktMapper.mapFastsettBruttoBeregningsgrunnlagSNDto(fastsettBruttoBeregningsgrunnlagSNDto));
        }
        if (dto instanceof VurderVarigEndringEllerNyoppstartetSNDto vurderVarigEndringEllerNyoppstartetSNDto) {
            return new VurderVarigEndringEllerNyoppstartetSNHåndteringDto(OppdatererKontraktMapper.mapVurderVarigEndringEllerNyoppstartetSNDto(vurderVarigEndringEllerNyoppstartetSNDto));
        }
        if (dto instanceof FordelBeregningsgrunnlagDto fordelBeregningsgrunnlagDto) {
            return new FaktaOmFordelingHåndteringDto(OppdatererKontraktMapper.mapFordelBeregningsgrunnlagDto(fordelBeregningsgrunnlagDto));
        }
        if (dto instanceof VurderRefusjonBeregningsgrunnlagDto) {
            return OppdatererKontraktMapper.mapVurderRefusjonBeregningsgrunnlag((VurderRefusjonBeregningsgrunnlagDto) dto);
        }
        throw new IllegalStateException("Aksjonspunkt er ikke mappet i kalkulus");
    }

    public static HåndterBeregningDto mapOverstyring(OverstyringAksjonspunktDto dto) {
        if (dto instanceof OverstyrBeregningsaktiviteterDto overstyrBeregningsaktiviteterDto) {
            var mappetDto = new no.nav.folketrygdloven.kalkulus.håndtering.v1.overstyring.OverstyrBeregningsaktiviteterDto(OppdatererKontraktMapper.mapOverstyrBeregningsaktiviteterDto(overstyrBeregningsaktiviteterDto.getBeregningsaktivitetLagreDtoList()));
            mappetDto.setBegrunnelse(dto.getBegrunnelse());
            return mappetDto;
        }
        if (dto instanceof OverstyrBeregningsgrunnlagDto overstyrBeregningsgrunnlagDto) {
            var mappetDto = new OverstyrBeregningsgrunnlagHåndteringDto(OppdatererKontraktMapper.mapTilFaktaOmBeregningLagreDto(overstyrBeregningsgrunnlagDto.getFakta()),
                OppdatererKontraktMapper.mapFastsettBeregningsgrunnlagPeriodeAndeler(overstyrBeregningsgrunnlagDto.getOverstyrteAndeler()));
            mappetDto.setBegrunnelse(dto.getBegrunnelse());
            return mappetDto;
        }
        throw new IllegalStateException("Overstyringaksjonspunkt er ikke mappet i kalkulus");

    }
}
