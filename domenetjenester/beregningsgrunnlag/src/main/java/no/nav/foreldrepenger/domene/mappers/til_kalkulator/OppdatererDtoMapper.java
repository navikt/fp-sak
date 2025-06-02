package no.nav.foreldrepenger.domene.mappers.til_kalkulator;

import java.util.Collections;
import java.util.List;

import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.ArbeidstakerandelUtenIMMottarYtelseDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.BesteberegningFødendeKvinneAndelDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.BesteberegningFødendeKvinneDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.DagpengeAndelLagtTilBesteberegningDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsattBrukersAndel;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.RedigerbarAndelFaktaOmBeregningDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.RefusjonskravPrArbeidsgiverVurderingDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderMilitærDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderteArbeidsforholdDto;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.dto.AvklarteAktiviteterDto;
import no.nav.foreldrepenger.domene.rest.dto.BeregningsaktivitetLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsatteAndelerTidsbegrensetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsattePerioderTidsbegrensetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsatteVerdierDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBgKunYtelseDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettEtterlønnSluttpakkeDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettInntektForArbeidUnderAAPDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettMånedsinntektFLDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettMånedsinntektUtenInntektsmeldingAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettMånedsinntektUtenInntektsmeldingDto;
import no.nav.foreldrepenger.domene.rest.dto.InntektPrAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.MottarYtelseDto;
import no.nav.foreldrepenger.domene.rest.dto.RedigerbarAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderATogFLiSammeOrganisasjonAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderATogFLiSammeOrganisasjonDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderEtterlønnSluttpakkeDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderLønnsendringDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderNyoppstartetFLDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonAndelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelFastsatteVerdierDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelRedigerbarAndelDto;

public class OppdatererDtoMapper {

    private OppdatererDtoMapper() {
    }

    public static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBeregningsgrunnlagATFLDto mapFastsettBeregningsgrunnlagATFLDto(FastsettBeregningsgrunnlagATFLDto tilKalkulus) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBeregningsgrunnlagATFLDto(mapTilInntektPrAndelListe(tilKalkulus.getInntektPrAndelList()), tilKalkulus.getInntektFrilanser());
    }

    public static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBGTidsbegrensetArbeidsforholdDto mapFastsettBGTidsbegrensetArbeidsforholdDto(FastsettBGTidsbegrensetArbeidsforholdDto tidsbegrensetDto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBGTidsbegrensetArbeidsforholdDto(
            tidsbegrensetDto.getFastsatteTidsbegrensedePerioder() == null ? null : mapTilFastsattTidsbegrensetPerioder(tidsbegrensetDto.getFastsatteTidsbegrensedePerioder()),
            tidsbegrensetDto.getFrilansInntekt());
    }

    public static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.AvklarteAktiviteterDto mapAvklarteAktiviteterDto(AvklarteAktiviteterDto dto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.AvklarteAktiviteterDto(mapTilBeregningsaktivitetLagreDtoList(dto.getBeregningsaktivitetLagreDtoList()));
    }

    public static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto mapFastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(dto.getBruttoBeregningsgrunnlag());
    }

    public static no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelBeregningsgrunnlagDto mapFordelBeregningsgrunnlagDto(FordelBeregningsgrunnlagDto dto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelBeregningsgrunnlagDto(mapTilEndredePerioderList(dto.getEndretBeregningsgrunnlagPerioder()));
    }

    public static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderRefusjonBeregningsgrunnlagDto mapVurderRefusjonBeregningsgrunnlag(VurderRefusjonBeregningsgrunnlagDto dto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderRefusjonBeregningsgrunnlagDto(mapTilFastsatteAndeler(dto.getFastsatteAndeler()));
    }

    private static List<no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderRefusjonAndelBeregningsgrunnlagDto> mapTilFastsatteAndeler(List<VurderRefusjonAndelBeregningsgrunnlagDto> fastsatteAndeler) {
        return fastsatteAndeler.stream()
            .map(a -> new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderRefusjonAndelBeregningsgrunnlagDto(
            a.getArbeidsgiverOrgnr(),
            a.getArbeidsgiverAktoerId(),
            a.getInternArbeidsforholdRef(),
            a.getFastsattRefusjonFom(),
            a.getDelvisRefusjonPrMndFørStart()))
            .toList();
    }

    public static List<no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.BeregningsaktivitetLagreDto> mapOverstyrBeregningsaktiviteterDto(List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList) {
        return beregningsaktivitetLagreDtoList.stream().map(OppdatererDtoMapper::mapTilBeregningsaktivitetLagreDto).toList();
    }

    public static OverstyrBeregningsgrunnlagDto mapOverstyrBeregningsgrunnlagDto(no.nav.foreldrepenger.domene.rest.dto.OverstyrBeregningsgrunnlagDto dto) {
        return new OverstyrBeregningsgrunnlagDto(mapFastsettBeregningsgrunnlagPeriodeAndeler(dto.getOverstyrteAndeler()), mapTilFaktaOmBeregningLagreDto(dto.getFakta()));
    }

    public static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FaktaBeregningLagreDto mapTilFaktaOmBeregningLagreDto(FaktaBeregningLagreDto fakta) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FaktaBeregningLagreDto(
            fakta.getVurderNyoppstartetFL() == null ? null : mapVurderNyoppstartetFLDto(fakta.getVurderNyoppstartetFL()),
            fakta.getVurderTidsbegrensetArbeidsforhold() == null ? null : mapTidsbegrensetArbeidsforhold(fakta.getVurderTidsbegrensetArbeidsforhold()),
            fakta.getVurderNyIArbeidslivet() == null ? null : mapVurderNyIArbeidslivet(fakta.getVurderNyIArbeidslivet()),
            fakta.getFastsettMaanedsinntektFL() == null ? null : mapFastsettMånedsinntektFL(fakta.getFastsettMaanedsinntektFL()),
            fakta.getVurdertLonnsendring() == null ? null : mapVurderLønnsendringDto(fakta.getVurdertLonnsendring()),
            fakta.getFastsattUtenInntektsmelding() == null ? null : mapFastsattUtenInntektsmeldingDto(fakta.getFastsattUtenInntektsmelding()),
            fakta.getVurderATogFLiSammeOrganisasjon() == null ? null : mapVurderAtOgFLiSammeOrganisasjonDto(fakta.getVurderATogFLiSammeOrganisasjon()),
            fakta.getBesteberegningAndeler() == null ? null : mapBesteberegningFødendeKvinneDto(fakta.getBesteberegningAndeler()),
            fakta.getFaktaOmBeregningTilfeller() == null ? null : mapFaktaOmBeregningTilfeller(fakta.getFaktaOmBeregningTilfeller()),
            fakta.getKunYtelseFordeling() == null ? null : mapFastsettKunYtelseDto(fakta.getKunYtelseFordeling()),
            fakta.getVurderEtterlønnSluttpakke() == null ? null : mapVurderEtterlønnSluttpakke(fakta.getVurderEtterlønnSluttpakke()),
            fakta.getFastsettEtterlønnSluttpakke() == null ? null : mapFastsettEtterlønnSluttpakker(fakta.getFastsettEtterlønnSluttpakke()),
            fakta.getMottarYtelse() == null ? null : mapMottarYtelse(fakta.getMottarYtelse()),
            fakta.getVurderMilitaer() == null ? null : mapVurderMilitær(fakta.getVurderMilitaer()),
            fakta.getRefusjonskravGyldighet() == null ? null : mapRefusjonskravPrArbeidsgiverVurderingDto(fakta.getRefusjonskravGyldighet()),
            fakta.getFastsettArbeidUnderAap() == null ? null : mapFastsettInntektForArbeidUnderAAP(fakta.getFastsettArbeidUnderAap())
        );
    }

    private static List<RefusjonskravPrArbeidsgiverVurderingDto> mapRefusjonskravPrArbeidsgiverVurderingDto(List<no.nav.foreldrepenger.domene.rest.dto.RefusjonskravPrArbeidsgiverVurderingDto> refusjonskravGyldighet) {
        return refusjonskravGyldighet.stream().map(OppdatererDtoMapper::mapRefusjonskravGyldighet).toList();
    }

    private static RefusjonskravPrArbeidsgiverVurderingDto mapRefusjonskravGyldighet(no.nav.foreldrepenger.domene.rest.dto.RefusjonskravPrArbeidsgiverVurderingDto refusjonskravPrArbeidsgiverVurderingDto) {
        return new RefusjonskravPrArbeidsgiverVurderingDto(
            refusjonskravPrArbeidsgiverVurderingDto.getArbeidsgiverId(),
            refusjonskravPrArbeidsgiverVurderingDto.isSkalUtvideGyldighet()
        );
    }

    private static VurderMilitærDto mapVurderMilitær(no.nav.foreldrepenger.domene.rest.dto.VurderMilitærDto vurderMilitaer) {
        return new VurderMilitærDto(vurderMilitaer.getHarMilitaer());
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.MottarYtelseDto mapMottarYtelse(MottarYtelseDto mottarYtelse) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.MottarYtelseDto(
            mottarYtelse.getFrilansMottarYtelse(),
            mottarYtelse.getArbeidstakerUtenIMMottarYtelse() == null ? null : mapArbeidstakterUtenIMMottarYtelseListe(mottarYtelse.getArbeidstakerUtenIMMottarYtelse()));
    }

    private static List<ArbeidstakerandelUtenIMMottarYtelseDto> mapArbeidstakterUtenIMMottarYtelseListe(List<no.nav.foreldrepenger.domene.rest.dto.ArbeidstakerandelUtenIMMottarYtelseDto> arbeidstakerUtenIMMottarYtelse) {
        return arbeidstakerUtenIMMottarYtelse.stream().map(OppdatererDtoMapper::mapArbeidstakterUtenIMMottarYtelse).toList();
    }

    private static ArbeidstakerandelUtenIMMottarYtelseDto mapArbeidstakterUtenIMMottarYtelse(no.nav.foreldrepenger.domene.rest.dto.ArbeidstakerandelUtenIMMottarYtelseDto arbeidstakerandelUtenIMMottarYtelseDto) {
        return new ArbeidstakerandelUtenIMMottarYtelseDto(
            arbeidstakerandelUtenIMMottarYtelseDto.getAndelsnr(),
            arbeidstakerandelUtenIMMottarYtelseDto.getMottarYtelse()
        );
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettEtterlønnSluttpakkeDto mapFastsettEtterlønnSluttpakker(FastsettEtterlønnSluttpakkeDto fastsettEtterlønnSluttpakke) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettEtterlønnSluttpakkeDto(fastsettEtterlønnSluttpakke.getFastsattPrMnd());
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderEtterlønnSluttpakkeDto mapVurderEtterlønnSluttpakke(VurderEtterlønnSluttpakkeDto vurderEtterlønnSluttpakke) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderEtterlønnSluttpakkeDto(
            vurderEtterlønnSluttpakke.erEtterlønnSluttpakke()
        );
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBgKunYtelseDto mapFastsettKunYtelseDto(FastsettBgKunYtelseDto kunYtelseFordeling) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBgKunYtelseDto(
            kunYtelseFordeling.getAndeler() == null ? null : mapKunYtelseAndeler(kunYtelseFordeling.getAndeler()),
            kunYtelseFordeling.getSkalBrukeBesteberegning()
        );
    }

    private static List<FastsattBrukersAndel> mapKunYtelseAndeler(List<no.nav.foreldrepenger.domene.rest.dto.FastsattBrukersAndel> andeler) {
        return andeler.stream().map(OppdatererDtoMapper::mapFastsattBrukersAndel).toList();
    }

    private static FastsattBrukersAndel mapFastsattBrukersAndel(no.nav.foreldrepenger.domene.rest.dto.FastsattBrukersAndel fastsattBrukersAndel) {
        return new FastsattBrukersAndel(
            fastsattBrukersAndel.getNyAndel(),
            fastsattBrukersAndel.getAndelsnr(),
            fastsattBrukersAndel.getLagtTilAvSaksbehandler(),
            fastsattBrukersAndel.getFastsattBeløp(),
            fastsattBrukersAndel.getInntektskategori() == null ? null : KodeverkTilKalkulusMapper.mapInntektskategori(fastsattBrukersAndel.getInntektskategori())
        );
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderATogFLiSammeOrganisasjonDto mapVurderAtOgFLiSammeOrganisasjonDto(VurderATogFLiSammeOrganisasjonDto vurderATogFLiSammeOrganisasjon) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderATogFLiSammeOrganisasjonDto(
            mapVurderAtOgFLiSammeOranisasjonAndelListe(vurderATogFLiSammeOrganisasjon.getVurderATogFLiSammeOrganisasjonAndelListe())
        );
    }

    private static List<no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderATogFLiSammeOrganisasjonAndelDto> mapVurderAtOgFLiSammeOranisasjonAndelListe(List<VurderATogFLiSammeOrganisasjonAndelDto> vurderATogFLiSammeOrganisasjonAndelListe) {
        return vurderATogFLiSammeOrganisasjonAndelListe.stream().map(OppdatererDtoMapper::mapVurderATOgFLiSammeOrgAndel).toList();
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderATogFLiSammeOrganisasjonAndelDto mapVurderATOgFLiSammeOrgAndel(VurderATogFLiSammeOrganisasjonAndelDto vurderATogFLiSammeOrganisasjonAndelDto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderATogFLiSammeOrganisasjonAndelDto(vurderATogFLiSammeOrganisasjonAndelDto.getAndelsnr(), vurderATogFLiSammeOrganisasjonAndelDto.getArbeidsinntekt());
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettMånedsinntektUtenInntektsmeldingDto mapFastsattUtenInntektsmeldingDto(FastsettMånedsinntektUtenInntektsmeldingDto fastsattUtenInntektsmelding) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettMånedsinntektUtenInntektsmeldingDto(mapFastsattUtenInntektsmeldingAndelListe(fastsattUtenInntektsmelding.getAndelListe()));
    }

    private static List<no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettMånedsinntektUtenInntektsmeldingAndelDto> mapFastsattUtenInntektsmeldingAndelListe(List<FastsettMånedsinntektUtenInntektsmeldingAndelDto> andelListe) {
        return andelListe.stream().map(OppdatererDtoMapper::mapAndel).toList();
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettMånedsinntektUtenInntektsmeldingAndelDto mapAndel(FastsettMånedsinntektUtenInntektsmeldingAndelDto fastsettMånedsinntektUtenInntektsmeldingAndelDto) {
        var fastsatteVerdier = no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsatteVerdierDto.Builder.ny()
            .medFastsattBeløpPrMnd(fastsettMånedsinntektUtenInntektsmeldingAndelDto.getFastsattBeløp())
            .medInntektskategori(fastsettMånedsinntektUtenInntektsmeldingAndelDto.getInntektskategori() == null ? null : KodeverkTilKalkulusMapper.mapInntektskategori(fastsettMånedsinntektUtenInntektsmeldingAndelDto.getInntektskategori()))
            .build();
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettMånedsinntektUtenInntektsmeldingAndelDto(
            fastsettMånedsinntektUtenInntektsmeldingAndelDto.getAndelsnr(),
            fastsatteVerdier
        );

    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderLønnsendringDto mapVurderLønnsendringDto(VurderLønnsendringDto vurdertLonnsendring) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderLønnsendringDto(vurdertLonnsendring.erLønnsendringIBeregningsperioden());
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettMånedsinntektFLDto mapFastsettMånedsinntektFL(FastsettMånedsinntektFLDto fastsettMaanedsinntektFL) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettMånedsinntektFLDto(fastsettMaanedsinntektFL.getMaanedsinntekt());
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto mapVurderNyIArbeidslivet(VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto vurderNyIArbeidslivet) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto(vurderNyIArbeidslivet.erNyIArbeidslivet());
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderTidsbegrensetArbeidsforholdDto mapTidsbegrensetArbeidsforhold(VurderTidsbegrensetArbeidsforholdDto vurderTidsbegrensetArbeidsforhold) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderTidsbegrensetArbeidsforholdDto(
            mapVurderteArbeidsforhold(vurderTidsbegrensetArbeidsforhold.getFastsatteArbeidsforhold())
        );
    }

    private static List<VurderteArbeidsforholdDto> mapVurderteArbeidsforhold(List<no.nav.foreldrepenger.domene.rest.dto.VurderteArbeidsforholdDto> fastsatteArbeidsforhold) {
        return fastsatteArbeidsforhold.stream().map(OppdatererDtoMapper::mapVurdertArbeidsforhold).toList();
    }

    private static VurderteArbeidsforholdDto mapVurdertArbeidsforhold(no.nav.foreldrepenger.domene.rest.dto.VurderteArbeidsforholdDto vurderteArbeidsforholdDto) {
        return new VurderteArbeidsforholdDto(
            vurderteArbeidsforholdDto.getAndelsnr(),
            vurderteArbeidsforholdDto.isTidsbegrensetArbeidsforhold()
        );
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderNyoppstartetFLDto mapVurderNyoppstartetFLDto(VurderNyoppstartetFLDto vurderNyoppstartetFL) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderNyoppstartetFLDto(vurderNyoppstartetFL.erErNyoppstartetFL());
    }

    private static BesteberegningFødendeKvinneDto mapBesteberegningFødendeKvinneDto(no.nav.foreldrepenger.domene.rest.dto.BesteberegningFødendeKvinneDto besteberegningAndeler) {
        return new BesteberegningFødendeKvinneDto(
            mapBesteberegningAndeler(besteberegningAndeler.getBesteberegningAndelListe()),
            besteberegningAndeler.getNyDagpengeAndel() == null ? null : mapNyDagpengeandel(besteberegningAndeler.getNyDagpengeAndel())
        );
    }

    private static DagpengeAndelLagtTilBesteberegningDto mapNyDagpengeandel(no.nav.foreldrepenger.domene.rest.dto.DagpengeAndelLagtTilBesteberegningDto nyDagpengeAndel) {
        return new DagpengeAndelLagtTilBesteberegningDto(nyDagpengeAndel.getFastsatteVerdier().getFastsattBeløp(), KodeverkTilKalkulusMapper.mapInntektskategori(nyDagpengeAndel.getFastsatteVerdier().getInntektskategori()));
    }

    private static List<BesteberegningFødendeKvinneAndelDto> mapBesteberegningAndeler(List<no.nav.foreldrepenger.domene.rest.dto.BesteberegningFødendeKvinneAndelDto> besteberegningAndelListe) {
        return besteberegningAndelListe.stream().map(OppdatererDtoMapper::mapBesteberegningAndel).toList();
    }

    private static BesteberegningFødendeKvinneAndelDto mapBesteberegningAndel(no.nav.foreldrepenger.domene.rest.dto.BesteberegningFødendeKvinneAndelDto besteberegningFødendeKvinneAndelDto) {
        return new BesteberegningFødendeKvinneAndelDto(
            besteberegningFødendeKvinneAndelDto.getAndelsnr(),
            besteberegningFødendeKvinneAndelDto.getFastsatteVerdier().getFastsattBeløp(),
            KodeverkTilKalkulusMapper.mapInntektskategori(besteberegningFødendeKvinneAndelDto.getFastsatteVerdier().getInntektskategori()),
            besteberegningFødendeKvinneAndelDto.getLagtTilAvSaksbehandler());
    }

    private static List<no.nav.folketrygdloven.kalkulus.kodeverk.FaktaOmBeregningTilfelle> mapFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> tilfeller) {
        return tilfeller.stream().map(KodeverkTilKalkulusMapper::mapFaktaBeregningTilfelle).toList();
    }


    private static List<no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.InntektPrAndelDto> mapTilInntektPrAndelListe(List<InntektPrAndelDto> inntektPrAndelList) {
        if (inntektPrAndelList == null) {
            return Collections.emptyList();
        }
        return inntektPrAndelList.stream().map(OppdatererDtoMapper::mapInntektPrAndel).toList();
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.InntektPrAndelDto mapInntektPrAndel(
        InntektPrAndelDto inntektPrAndelDto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.InntektPrAndelDto(inntektPrAndelDto.getInntekt(), inntektPrAndelDto.getAndelsnr());
    }

    private static List<no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsattePerioderTidsbegrensetDto> mapTilFastsattTidsbegrensetPerioder(List<FastsattePerioderTidsbegrensetDto> fastsatteTidsbegrensedePerioder) {
        return fastsatteTidsbegrensedePerioder.stream().map(OppdatererDtoMapper::mapTilFastsattTidsbegrensetPeriode).toList();
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsattePerioderTidsbegrensetDto mapTilFastsattTidsbegrensetPeriode(FastsattePerioderTidsbegrensetDto fastsattePerioderTidsbegrensetDto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsattePerioderTidsbegrensetDto(fastsattePerioderTidsbegrensetDto.getPeriodeFom(), fastsattePerioderTidsbegrensetDto.getPeriodeTom(), mapTilFastsattTidsbegrensetAndeler(fastsattePerioderTidsbegrensetDto.getFastsatteTidsbegrensedeAndeler()));
    }

    private static List<no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsatteAndelerTidsbegrensetDto> mapTilFastsattTidsbegrensetAndeler(List<FastsatteAndelerTidsbegrensetDto> fastsatteTidsbegrensedeAndeler) {
        return fastsatteTidsbegrensedeAndeler.stream().map(OppdatererDtoMapper::mapTilFastsattTidsbegrensetAndel).toList();
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsatteAndelerTidsbegrensetDto mapTilFastsattTidsbegrensetAndel(FastsatteAndelerTidsbegrensetDto fastsatteAndelerTidsbegrensetDto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsatteAndelerTidsbegrensetDto(fastsatteAndelerTidsbegrensetDto.getAndelsnr(), fastsatteAndelerTidsbegrensetDto.getBruttoFastsattInntekt());
    }

    private static List<no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.BeregningsaktivitetLagreDto> mapTilBeregningsaktivitetLagreDtoList(List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList) {
        return beregningsaktivitetLagreDtoList.stream().map(OppdatererDtoMapper::mapTilBeregningsaktivitetLagreDto).toList();
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.BeregningsaktivitetLagreDto mapTilBeregningsaktivitetLagreDto(BeregningsaktivitetLagreDto beregningsaktivitetLagreDto) {
        return no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.BeregningsaktivitetLagreDto.builder()
            .medArbeidsforholdRef(beregningsaktivitetLagreDto.getArbeidsforholdRef())
            .medArbeidsgiverIdentifikator(beregningsaktivitetLagreDto.getArbeidsgiverIdentifikator())
            .medFom(beregningsaktivitetLagreDto.getFom())
            .medOppdragsgiverOrg(beregningsaktivitetLagreDto.getOppdragsgiverOrg())
            .medOpptjeningAktivitetType(KodeverkTilKalkulusMapper.mapOpptjeningAktivitetType(beregningsaktivitetLagreDto.getOpptjeningAktivitetType()))
            .medSkalBrukes(beregningsaktivitetLagreDto.getSkalBrukes())
            .medTom(beregningsaktivitetLagreDto.getTom())
            .build();
    }

    private static List<no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelBeregningsgrunnlagPeriodeDto> mapTilEndredePerioderList(List<FordelBeregningsgrunnlagPeriodeDto> endretBeregningsgrunnlagPerioder) {
        return endretBeregningsgrunnlagPerioder.stream().map(OppdatererDtoMapper::mapTilFordelBeregningsgrunnlagPeriodeDto).toList();
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelBeregningsgrunnlagPeriodeDto mapTilFordelBeregningsgrunnlagPeriodeDto(FordelBeregningsgrunnlagPeriodeDto fastsettBeregningsgrunnlagPeriodeDto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelBeregningsgrunnlagPeriodeDto(mapFordelBeregningsgrunnlagPeriodeAndeler(fastsettBeregningsgrunnlagPeriodeDto.getAndeler()), fastsettBeregningsgrunnlagPeriodeDto.getFom(), fastsettBeregningsgrunnlagPeriodeDto.getTom());
    }

    private static List<no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelBeregningsgrunnlagAndelDto> mapFordelBeregningsgrunnlagPeriodeAndeler(List<FordelBeregningsgrunnlagAndelDto> andeler) {
        return andeler.stream().map(OppdatererDtoMapper::mapFordelBeregningsgrunnlagPeriodeAndelDto).toList();
    }

    private static List<no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBeregningsgrunnlagAndelDto> mapFastsettBeregningsgrunnlagPeriodeAndeler(List<FastsettBeregningsgrunnlagAndelDto> andeler) {
        return andeler.stream().map(OppdatererDtoMapper::mapFastsettBeregningsgrunnlagPeriodeAndelDto).toList();
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelBeregningsgrunnlagAndelDto mapFordelBeregningsgrunnlagPeriodeAndelDto(FordelBeregningsgrunnlagAndelDto fastsettBeregningsgrunnlagAndelDto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelBeregningsgrunnlagAndelDto(
            mapTilFordelRedigerbarAndelDto(fastsettBeregningsgrunnlagAndelDto),
            mapTilFordelFastsatteVerdier(fastsettBeregningsgrunnlagAndelDto.getFastsatteVerdier()),
            fastsettBeregningsgrunnlagAndelDto.getForrigeInntektskategori() == null ? null : KodeverkTilKalkulusMapper.mapInntektskategori(fastsettBeregningsgrunnlagAndelDto.getForrigeInntektskategori()),
            fastsettBeregningsgrunnlagAndelDto.getForrigeRefusjonPrÅr(),
            fastsettBeregningsgrunnlagAndelDto.getForrigeArbeidsinntektPrÅr());
    }


    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBeregningsgrunnlagAndelDto mapFastsettBeregningsgrunnlagPeriodeAndelDto(FastsettBeregningsgrunnlagAndelDto fastsettBeregningsgrunnlagAndelDto) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBeregningsgrunnlagAndelDto(
            mapTilRedigerbarAndelDto(fastsettBeregningsgrunnlagAndelDto),
            mapTilFastsatteVerdier(fastsettBeregningsgrunnlagAndelDto.getFastsatteVerdier()),
            fastsettBeregningsgrunnlagAndelDto.getForrigeInntektskategori() == null ? null : KodeverkTilKalkulusMapper.mapInntektskategori(fastsettBeregningsgrunnlagAndelDto.getForrigeInntektskategori()),
            fastsettBeregningsgrunnlagAndelDto.getForrigeRefusjonPrÅr(),
            fastsettBeregningsgrunnlagAndelDto.getForrigeArbeidsinntektPrÅr());
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelFastsatteVerdierDto mapTilFordelFastsatteVerdier(FordelFastsatteVerdierDto fastsatteVerdier) {
        return no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelFastsatteVerdierDto.Builder.ny()
            .medInntektskategori(fastsatteVerdier.getInntektskategori() == null ? null : KodeverkTilKalkulusMapper.mapInntektskategori(fastsatteVerdier.getInntektskategori()))
            .medFastsattBeløpPrÅrInklNaturalytelse(fastsatteVerdier.getFastsattÅrsbeløpInklNaturalytelse())
            .medRefusjonPrÅr(fastsatteVerdier.getRefusjonPrÅr())
            .build();
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsatteVerdierDto mapTilFastsatteVerdier(FastsatteVerdierDto fastsatteVerdier) {
        return no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsatteVerdierDto.Builder.ny()
            .medSkalHaBesteberegning(fastsatteVerdier.getSkalHaBesteberegning())
            .medInntektskategori(fastsatteVerdier.getInntektskategori() == null ? null : KodeverkTilKalkulusMapper.mapInntektskategori(fastsatteVerdier.getInntektskategori()))
            .medFastsattBeløpPrMnd(fastsatteVerdier.getFastsattBeløp())
            .medFastsattBeløpPrÅr(fastsatteVerdier.getFastsattÅrsbeløp())
            .build();
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.RedigerbarAndelDto mapTilFordelRedigerbarAndelDto(FordelRedigerbarAndelDto redigerbarAndel) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.RedigerbarAndelDto(
            redigerbarAndel.getAndelsnr(),
            redigerbarAndel.getArbeidsgiverId(),
            redigerbarAndel.getArbeidsforholdId().getReferanse(),
            redigerbarAndel.getNyAndel(),
            redigerbarAndel.getKilde() == null ? null : KodeverkTilKalkulusMapper.mapAndelkilde(redigerbarAndel.getKilde()));
    }

    private static RedigerbarAndelFaktaOmBeregningDto mapTilRedigerbarAndelDto(RedigerbarAndelDto redigerbarAndel) {
        return new RedigerbarAndelFaktaOmBeregningDto(
            redigerbarAndel.getAndelsnr(),
            redigerbarAndel.getNyAndel(),
            redigerbarAndel.getAktivitetStatus() == null ? null : KodeverkTilKalkulusMapper.mapAktivitetstatus(redigerbarAndel.getAktivitetStatus()),
            redigerbarAndel.getLagtTilAvSaksbehandler());
    }

    private static no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettInntektForArbeidUnderAAPDto mapFastsettInntektForArbeidUnderAAP(
        FastsettInntektForArbeidUnderAAPDto fastsettInntektForArbeidUnderAAP) {
        return new no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettInntektForArbeidUnderAAPDto(fastsettInntektForArbeidUnderAAP.getFastsattPrMnd());
    }
}
