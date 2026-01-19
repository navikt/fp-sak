package no.nav.foreldrepenger.domene.aksjonspunkt;

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
import no.nav.foreldrepenger.kalkulus.kontrakt.request.håndtering.HåndterBeregningDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.håndtering.avklaraktiviteter.AvklarAktiviteterHåndteringDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.håndtering.fakta.FaktaOmBeregningHåndteringDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.håndtering.fordeling.FaktaOmFordelingHåndteringDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.håndtering.foreslå.FastsettBGTidsbegrensetArbeidsforholdHåndteringDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.håndtering.foreslå.FastsettBeregningsgrunnlagATFLHåndteringDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.håndtering.foreslå.FastsettBeregningsgrunnlagSNNyIArbeidslivetHåndteringDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.håndtering.foreslå.VurderVarigEndringEllerNyoppstartetSNHåndteringDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.håndtering.overstyring.OverstyrBeregningsgrunnlagHåndteringDto;

import java.util.List;

public class KalkulusAksjonspunktMapper {

    private KalkulusAksjonspunktMapper() {
        // Hindrer instansiering
    }


    public static HåndterBeregningDto mapAksjonspunktTilKalkulusDto(BekreftetAksjonspunktDto dto) {
        var kalkulusDto = mapSpesifikkDto(dto);
        // Begrunnelse lagres uansett i fpsak, men kalkulus vil gjerne ha den for å vise saksbehandler i beregningsbildene
        kalkulusDto.setBegrunnelse(dto.getBegrunnelse());
        return kalkulusDto;
    }

    private static HåndterBeregningDto mapSpesifikkDto(BekreftetAksjonspunktDto dto) {
        if (dto instanceof AvklarteAktiviteterDto avklarteAktiviteterDto) {
            return new AvklarAktiviteterHåndteringDto(OppdatererDtoMapper.mapAvklarteAktiviteterDto(avklarteAktiviteterDto));
        }
        if (dto instanceof VurderFaktaOmBeregningDto faktaOmBeregningDto) {
            return new FaktaOmBeregningHåndteringDto(OppdatererDtoMapper.mapTilFaktaOmBeregningLagreDto(faktaOmBeregningDto.getFakta()));
        }
        if (dto instanceof FastsettBeregningsgrunnlagATFLDto fastsettBeregningsgrunnlagATFLDto) {
            return new FastsettBeregningsgrunnlagATFLHåndteringDto(OppdatererDtoMapper.mapTilInntektPrAndelListe(fastsettBeregningsgrunnlagATFLDto.getInntektPrAndelList()), fastsettBeregningsgrunnlagATFLDto.getInntektFrilanser(), List.of());
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

    public static HåndterBeregningDto mapOverstyringTilKalkulusDto(OverstyringAksjonspunktDto overstyring) {
        var kalkulusDto = mapSpesifikkOverstyring(overstyring);
        kalkulusDto.setBegrunnelse(overstyring.getBegrunnelse());
        return kalkulusDto;
    }

    private static HåndterBeregningDto mapSpesifikkOverstyring(OverstyringAksjonspunktDto dto) {
        if (dto instanceof OverstyrBeregningsaktiviteterDto overstyrBeregningsaktiviteterDto) {
            var mappetDto = new no.nav.foreldrepenger.kalkulus.kontrakt.request.håndtering.overstyring.OverstyrBeregningsaktiviteterDto(OppdatererDtoMapper.mapOverstyrBeregningsaktiviteterDto(overstyrBeregningsaktiviteterDto.getBeregningsaktivitetLagreDtoList()));
            mappetDto.setBegrunnelse(dto.getBegrunnelse());
            return mappetDto;
        }
        if (dto instanceof OverstyrBeregningsgrunnlagDto overstyrBeregningsgrunnlagDto) {
            var mappetDto = new OverstyrBeregningsgrunnlagHåndteringDto(OppdatererDtoMapper.mapTilFaktaOmBeregningLagreDto(overstyrBeregningsgrunnlagDto.getFakta()),
                OppdatererDtoMapper.mapFastsettBeregningsgrunnlagPeriodeAndeler(overstyrBeregningsgrunnlagDto.getOverstyrteAndeler()));
            mappetDto.setBegrunnelse(dto.getBegrunnelse());
            return mappetDto;
        }
        throw new IllegalStateException("Overstyring er ikke mappet i kalkulus");
    }

}
