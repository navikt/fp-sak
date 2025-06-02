package no.nav.foreldrepenger.domene.aksjonspunkt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FaktaOmBeregningTilfelleDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.AndelKilde;
import no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.KodeverkTilKalkulusMapper;
import no.nav.foreldrepenger.domene.rest.dto.ArbeidstakerandelUtenIMMottarYtelseDto;
import no.nav.foreldrepenger.domene.rest.dto.AvklarteAktiviteterDto;
import no.nav.foreldrepenger.domene.rest.dto.BeregningsaktivitetLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.BesteberegningFødendeKvinneAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.BesteberegningFødendeKvinneDto;
import no.nav.foreldrepenger.domene.rest.dto.DagpengeAndelLagtTilBesteberegningDto;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsattBrukersAndel;
import no.nav.foreldrepenger.domene.rest.dto.FastsatteAndelerTidsbegrensetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsattePerioderTidsbegrensetDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsatteVerdierDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
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
import no.nav.foreldrepenger.domene.rest.dto.RefusjonskravPrArbeidsgiverVurderingDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderATogFLiSammeOrganisasjonAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderATogFLiSammeOrganisasjonDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderEtterlønnSluttpakkeDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderLønnsendringDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderMilitærDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderNyoppstartetFLDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonAndelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderTidsbegrensetArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderteArbeidsforholdDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelFastsatteVerdierDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelRedigerbarAndelDto;


public class OppdatererDtoMapper {

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBGTidsbegrensetArbeidsforholdDto mapFastsettBGTidsbegrensetArbeidsforholdDto(
        FastsettBGTidsbegrensetArbeidsforholdDto tidsbegrensetDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBGTidsbegrensetArbeidsforholdDto(
            tidsbegrensetDto.getFastsatteTidsbegrensedePerioder() == null ? null : mapTilFastsattTidsbegrensetPerioder(tidsbegrensetDto.getFastsatteTidsbegrensedePerioder()),
            tidsbegrensetDto.getFrilansInntekt());
    }

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.AvklarteAktiviteterDto mapAvklarteAktiviteterDto(
        AvklarteAktiviteterDto dto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.AvklarteAktiviteterDto(mapTilBeregningsaktivitetLagreDtoList(dto.getBeregningsaktivitetLagreDtoList()));
    }

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto mapFastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(
        FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto(dto.getBruttoBeregningsgrunnlag());
    }

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagDto mapFordelBeregningsgrunnlagDto(
        FordelBeregningsgrunnlagDto dto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagDto(mapTilEndredePerioderList(dto.getEndretBeregningsgrunnlagPerioder()));
    }

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.refusjon.VurderRefusjonBeregningsgrunnlagDto mapVurderRefusjonBeregningsgrunnlag(
        VurderRefusjonBeregningsgrunnlagDto dto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.refusjon.VurderRefusjonBeregningsgrunnlagDto(mapTilRefusjonAndeler(dto.getFastsatteAndeler()));
    }

    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.VurderVarigEndringEllerNyoppstartetDto mapVurderVarigEndringEllerNyoppstartetDto(
        VurderVarigEndringEllerNyoppstartetSNDto dto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.VurderVarigEndringEllerNyoppstartetDto(dto.getErVarigEndretNaering(), dto.getBruttoBeregningsgrunnlag());
    }


    public static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.BeregningsaktivitetLagreDto> mapOverstyrBeregningsaktiviteterDto(List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList) {
        return beregningsaktivitetLagreDtoList.stream().map(OppdatererDtoMapper::mapTilBeregningsaktivitetLagreDto).collect(Collectors.toList());
    }


    public static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FaktaBeregningLagreDto mapTilFaktaOmBeregningLagreDto(FaktaBeregningLagreDto fakta) {
        return fakta == null || fakta.getFaktaOmBeregningTilfeller().isEmpty() ? null : new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FaktaBeregningLagreDto(
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

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.RefusjonskravPrArbeidsgiverVurderingDto> mapRefusjonskravPrArbeidsgiverVurderingDto(List<RefusjonskravPrArbeidsgiverVurderingDto> refusjonskravGyldighet) {
        return refusjonskravGyldighet.stream().map(OppdatererDtoMapper::mapRefusjonskravGyldighet).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.RefusjonskravPrArbeidsgiverVurderingDto mapRefusjonskravGyldighet(RefusjonskravPrArbeidsgiverVurderingDto refusjonskravPrArbeidsgiverVurderingDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.RefusjonskravPrArbeidsgiverVurderingDto(
            refusjonskravPrArbeidsgiverVurderingDto.getArbeidsgiverId(),
            refusjonskravPrArbeidsgiverVurderingDto.isSkalUtvideGyldighet()
            );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderMilitærDto mapVurderMilitær(VurderMilitærDto vurderMilitaer) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderMilitærDto(vurderMilitaer.getHarMilitaer());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.MottarYtelseDto mapMottarYtelse(MottarYtelseDto mottarYtelse) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.MottarYtelseDto(
            mottarYtelse.getFrilansMottarYtelse(),
            mottarYtelse.getArbeidstakerUtenIMMottarYtelse() == null ? null : mapArbeidstakterUtenIMMottarYtelseListe(mottarYtelse.getArbeidstakerUtenIMMottarYtelse()));
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.ArbeidstakerandelUtenIMMottarYtelseDto> mapArbeidstakterUtenIMMottarYtelseListe(List<ArbeidstakerandelUtenIMMottarYtelseDto> arbeidstakerUtenIMMottarYtelse) {
        return arbeidstakerUtenIMMottarYtelse.stream().map(OppdatererDtoMapper::mapArbeidstakterUtenIMMottarYtelse).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.ArbeidstakerandelUtenIMMottarYtelseDto mapArbeidstakterUtenIMMottarYtelse(ArbeidstakerandelUtenIMMottarYtelseDto arbeidstakerandelUtenIMMottarYtelseDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.ArbeidstakerandelUtenIMMottarYtelseDto(
            arbeidstakerandelUtenIMMottarYtelseDto.getAndelsnr(),
            arbeidstakerandelUtenIMMottarYtelseDto.getMottarYtelse()
        );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettEtterlønnSluttpakkeDto mapFastsettEtterlønnSluttpakker(
        FastsettEtterlønnSluttpakkeDto fastsettEtterlønnSluttpakke) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettEtterlønnSluttpakkeDto(fastsettEtterlønnSluttpakke.getFastsattPrMnd());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderEtterlønnSluttpakkeDto mapVurderEtterlønnSluttpakke(
        VurderEtterlønnSluttpakkeDto vurderEtterlønnSluttpakke) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderEtterlønnSluttpakkeDto(
            vurderEtterlønnSluttpakke.erEtterlønnSluttpakke()
        );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettBgKunYtelseDto mapFastsettKunYtelseDto(FastsettBgKunYtelseDto kunYtelseFordeling) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettBgKunYtelseDto(
            mapKunYtelseAndeler(kunYtelseFordeling.getAndeler()),
            kunYtelseFordeling.getSkalBrukeBesteberegning()
        );
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattBrukersAndel> mapKunYtelseAndeler(List<FastsattBrukersAndel> andeler) {
        return andeler.stream().map(OppdatererDtoMapper::mapFastsattBrukersAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattBrukersAndel mapFastsattBrukersAndel(FastsattBrukersAndel fastsattBrukersAndel) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattBrukersAndel(
            fastsattBrukersAndel.getAndelsnr(),
            fastsattBrukersAndel.getNyAndel(),
            fastsattBrukersAndel.getLagtTilAvSaksbehandler(),
            fastsattBrukersAndel.getFastsattBeløp(),
            fastsattBrukersAndel.getInntektskategori() == null ?
                null : Inntektskategori.fraKode(fastsattBrukersAndel.getInntektskategori().getKode())
        );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderATogFLiSammeOrganisasjonDto mapVurderAtOgFLiSammeOrganisasjonDto(
        VurderATogFLiSammeOrganisasjonDto vurderATogFLiSammeOrganisasjon) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderATogFLiSammeOrganisasjonDto(
            mapVurderAtOgFLiSammeOranisasjonAndelListe(vurderATogFLiSammeOrganisasjon.getVurderATogFLiSammeOrganisasjonAndelListe())
        );
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderATogFLiSammeOrganisasjonAndelDto> mapVurderAtOgFLiSammeOranisasjonAndelListe(List<VurderATogFLiSammeOrganisasjonAndelDto> vurderATogFLiSammeOrganisasjonAndelListe) {
        return vurderATogFLiSammeOrganisasjonAndelListe.stream().map(OppdatererDtoMapper::mapVurderATOgFLiSammeOrgAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderATogFLiSammeOrganisasjonAndelDto mapVurderATOgFLiSammeOrgAndel(VurderATogFLiSammeOrganisasjonAndelDto vurderATogFLiSammeOrganisasjonAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderATogFLiSammeOrganisasjonAndelDto(vurderATogFLiSammeOrganisasjonAndelDto.getAndelsnr(), vurderATogFLiSammeOrganisasjonAndelDto.getArbeidsinntekt());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektUtenInntektsmeldingDto mapFastsattUtenInntektsmeldingDto(
        FastsettMånedsinntektUtenInntektsmeldingDto fastsattUtenInntektsmelding) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektUtenInntektsmeldingDto(mapFastsattUtenInntektsmeldingAndelListe(fastsattUtenInntektsmelding.getAndelListe()));
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektUtenInntektsmeldingAndelDto> mapFastsattUtenInntektsmeldingAndelListe(List<FastsettMånedsinntektUtenInntektsmeldingAndelDto> andelListe) {
        return andelListe.stream().map(OppdatererDtoMapper::mapAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektUtenInntektsmeldingAndelDto mapAndel(FastsettMånedsinntektUtenInntektsmeldingAndelDto fastsettMånedsinntektUtenInntektsmeldingAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektUtenInntektsmeldingAndelDto(
            fastsettMånedsinntektUtenInntektsmeldingAndelDto.getAndelsnr(),
                fastsettMånedsinntektUtenInntektsmeldingAndelDto.getFastsattBeløp(),
                fastsettMånedsinntektUtenInntektsmeldingAndelDto.getInntektskategori() == null ?  null :
                Inntektskategori.fraKode(fastsettMånedsinntektUtenInntektsmeldingAndelDto.getInntektskategori().getKode()));
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderLønnsendringDto mapVurderLønnsendringDto(VurderLønnsendringDto vurdertLonnsendring) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderLønnsendringDto(vurdertLonnsendring.erLønnsendringIBeregningsperioden());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektFLDto mapFastsettMånedsinntektFL(
        FastsettMånedsinntektFLDto fastsettMaanedsinntektFL) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettMånedsinntektFLDto(fastsettMaanedsinntektFL.getMaanedsinntekt());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto mapVurderNyIArbeidslivet(
        VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto vurderNyIArbeidslivet) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto(vurderNyIArbeidslivet.erNyIArbeidslivet());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderTidsbegrensetArbeidsforholdDto mapTidsbegrensetArbeidsforhold(
        VurderTidsbegrensetArbeidsforholdDto vurderTidsbegrensetArbeidsforhold) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderTidsbegrensetArbeidsforholdDto(
            mapVurderteArbeidsforhold(vurderTidsbegrensetArbeidsforhold.getFastsatteArbeidsforhold())
        );
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderteArbeidsforholdDto> mapVurderteArbeidsforhold(List<VurderteArbeidsforholdDto> fastsatteArbeidsforhold) {
        return fastsatteArbeidsforhold.stream().map(OppdatererDtoMapper::mapVurdertArbeidsforhold).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderteArbeidsforholdDto mapVurdertArbeidsforhold(VurderteArbeidsforholdDto vurderteArbeidsforholdDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderteArbeidsforholdDto(
            vurderteArbeidsforholdDto.getAndelsnr(),
            vurderteArbeidsforholdDto.isTidsbegrensetArbeidsforhold()
        );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderNyoppstartetFLDto mapVurderNyoppstartetFLDto(VurderNyoppstartetFLDto vurderNyoppstartetFLDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.VurderNyoppstartetFLDto(vurderNyoppstartetFLDto.erErNyoppstartetFL());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.BesteberegningFødendeKvinneDto mapBesteberegningFødendeKvinneDto(
        BesteberegningFødendeKvinneDto besteberegningAndeler) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.BesteberegningFødendeKvinneDto(
            mapBesteberegningAndeler(besteberegningAndeler.getBesteberegningAndelListe()),
            besteberegningAndeler.getNyDagpengeAndel() == null ? null : mapNyDagpengeandel(besteberegningAndeler.getNyDagpengeAndel())
            );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.DagpengeAndelLagtTilBesteberegningDto mapNyDagpengeandel(
        DagpengeAndelLagtTilBesteberegningDto nyDagpengeAndel) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.DagpengeAndelLagtTilBesteberegningDto(nyDagpengeAndel.getFastsatteVerdier().getFastsattBeløp(),
            Inntektskategori.fraKode(nyDagpengeAndel.getFastsatteVerdier().getInntektskategori().getKode()));
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.BesteberegningFødendeKvinneAndelDto> mapBesteberegningAndeler(List<BesteberegningFødendeKvinneAndelDto> besteberegningAndelListe) {
        return besteberegningAndelListe.stream().map(OppdatererDtoMapper::mapBesteberegningAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.BesteberegningFødendeKvinneAndelDto mapBesteberegningAndel(BesteberegningFødendeKvinneAndelDto besteberegningFødendeKvinneAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.BesteberegningFødendeKvinneAndelDto(
            besteberegningFødendeKvinneAndelDto.getAndelsnr(),
            besteberegningFødendeKvinneAndelDto.getFastsatteVerdier().getFastsattBeløp(),
            Inntektskategori.fraKode(besteberegningFødendeKvinneAndelDto.getFastsatteVerdier().getInntektskategori().getKode()),
            besteberegningFødendeKvinneAndelDto.getLagtTilAvSaksbehandler());
    }

    private static FaktaOmBeregningTilfelleDto mapFaktaOmBeregningTilfeller(List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> tilfeller) {
        return new FaktaOmBeregningTilfelleDto(
            tilfeller.stream().map(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle::getKode).map(no.nav.folketrygdloven.kalkulus.kodeverk.FaktaOmBeregningTilfelle::fraKode).collect(Collectors.toList()));
    }

    public static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.InntektPrAndelDto> mapTilInntektPrAndelListe(List<InntektPrAndelDto> inntektPrAndelList) {
        if(inntektPrAndelList == null){
            return Collections.emptyList();
        }
        return inntektPrAndelList.stream().map(OppdatererDtoMapper::mapInntektPrAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.InntektPrAndelDto mapInntektPrAndel(InntektPrAndelDto inntektPrAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.foreslå.InntektPrAndelDto(inntektPrAndelDto.getInntekt(), inntektPrAndelDto.getAndelsnr());
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattePerioderTidsbegrensetDto> mapTilFastsattTidsbegrensetPerioder(List<FastsattePerioderTidsbegrensetDto> fastsatteTidsbegrensedePerioder) {
        return fastsatteTidsbegrensedePerioder.stream().map(OppdatererDtoMapper::mapTilFastsattTidsbegrensetPeriode).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattePerioderTidsbegrensetDto mapTilFastsattTidsbegrensetPeriode(FastsattePerioderTidsbegrensetDto fastsattePerioderTidsbegrensetDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsattePerioderTidsbegrensetDto(fastsattePerioderTidsbegrensetDto.getPeriodeFom(), fastsattePerioderTidsbegrensetDto.getPeriodeTom(), mapTilFastsattTidsbegrensetAndeler(fastsattePerioderTidsbegrensetDto.getFastsatteTidsbegrensedeAndeler()));
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsatteAndelerTidsbegrensetDto> mapTilFastsattTidsbegrensetAndeler(List<FastsatteAndelerTidsbegrensetDto> fastsatteTidsbegrensedeAndeler) {
        return fastsatteTidsbegrensedeAndeler.stream().map(OppdatererDtoMapper::mapTilFastsattTidsbegrensetAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsatteAndelerTidsbegrensetDto mapTilFastsattTidsbegrensetAndel(FastsatteAndelerTidsbegrensetDto fastsatteAndelerTidsbegrensetDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsatteAndelerTidsbegrensetDto(fastsatteAndelerTidsbegrensetDto.getAndelsnr(), fastsatteAndelerTidsbegrensetDto.getBruttoFastsattInntekt());
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.BeregningsaktivitetLagreDto> mapTilBeregningsaktivitetLagreDtoList(List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList) {
        return beregningsaktivitetLagreDtoList.stream().map(OppdatererDtoMapper::mapTilBeregningsaktivitetLagreDto).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.BeregningsaktivitetLagreDto mapTilBeregningsaktivitetLagreDto(BeregningsaktivitetLagreDto beregningsaktivitetLagreDto) {
        return no.nav.folketrygdloven.kalkulus.håndtering.v1.avklaraktiviteter.BeregningsaktivitetLagreDto.builder()
            .medArbeidsforholdRef(beregningsaktivitetLagreDto.getArbeidsforholdRef() == null ? null : UUID.fromString(beregningsaktivitetLagreDto.getArbeidsforholdRef()))
            .medArbeidsgiverIdentifikator(beregningsaktivitetLagreDto.getArbeidsgiverIdentifikator())
            .medFom(beregningsaktivitetLagreDto.getFom())
            .medOppdragsgiverOrg(beregningsaktivitetLagreDto.getOppdragsgiverOrg())
            .medOpptjeningAktivitetType(KodeverkTilKalkulusMapper.mapOpptjeningAktivitetType(beregningsaktivitetLagreDto.getOpptjeningAktivitetType()))
            .medSkalBrukes(beregningsaktivitetLagreDto.getSkalBrukes())
            .medTom(beregningsaktivitetLagreDto.getTom())
            .build();
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.refusjon.VurderRefusjonAndelBeregningsgrunnlagDto> mapTilRefusjonAndeler(List<VurderRefusjonAndelBeregningsgrunnlagDto> andelListe) {
        return andelListe.stream().map(OppdatererDtoMapper::mapRefusjonAndel).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.refusjon.VurderRefusjonAndelBeregningsgrunnlagDto mapRefusjonAndel(VurderRefusjonAndelBeregningsgrunnlagDto andel) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.refusjon.VurderRefusjonAndelBeregningsgrunnlagDto(
            andel.getArbeidsgiverOrgnr(),
            andel.getArbeidsgiverAktoerId(),
            andel.getInternArbeidsforholdRef(),
            andel.getFastsattRefusjonFom(),
            andel.getDelvisRefusjonPrMndFørStart());
    }


    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagPeriodeDto> mapTilEndredePerioderList(List<FordelBeregningsgrunnlagPeriodeDto> endretBeregningsgrunnlagPerioder) {
        return endretBeregningsgrunnlagPerioder.stream().map(OppdatererDtoMapper::mapTilFordelBeregningsgrunnlagPeriodeDto).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagPeriodeDto mapTilFordelBeregningsgrunnlagPeriodeDto(FordelBeregningsgrunnlagPeriodeDto fordelBeregningsgrunnlagPeriodeDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagPeriodeDto(mapFordelBeregningsgrunnlagPeriodeAndeler(fordelBeregningsgrunnlagPeriodeDto.getAndeler()), fordelBeregningsgrunnlagPeriodeDto.getFom(), fordelBeregningsgrunnlagPeriodeDto.getTom());
    }

    private static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagAndelDto> mapFordelBeregningsgrunnlagPeriodeAndeler(List<FordelBeregningsgrunnlagAndelDto> andeler) {
        return andeler.stream().map(OppdatererDtoMapper::mapFordelBeregningsgrunnlagPeriodeAndelDto).collect(Collectors.toList());
    }

    public static List<no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettBeregningsgrunnlagAndelDto> mapFastsettBeregningsgrunnlagPeriodeAndeler(List<FastsettBeregningsgrunnlagAndelDto> andeler) {
        return andeler.stream().map(OppdatererDtoMapper::mapFastsettBeregningsgrunnlagPeriodeAndelDto).collect(Collectors.toList());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagAndelDto mapFordelBeregningsgrunnlagPeriodeAndelDto(FordelBeregningsgrunnlagAndelDto fordelBeregningsgrunnlagAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelBeregningsgrunnlagAndelDto(
            mapTilFordelRedigerbarAndelDto(fordelBeregningsgrunnlagAndelDto),
            mapTilFordelFastsatteVerdier(fordelBeregningsgrunnlagAndelDto.getFastsatteVerdier()),
            fordelBeregningsgrunnlagAndelDto.getForrigeInntektskategori() == null ? null : Inntektskategori.fraKode(fordelBeregningsgrunnlagAndelDto.getForrigeInntektskategori().getKode()),
            fordelBeregningsgrunnlagAndelDto.getForrigeRefusjonPrÅr(),
            fordelBeregningsgrunnlagAndelDto.getForrigeArbeidsinntektPrÅr());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettBeregningsgrunnlagAndelDto mapFastsettBeregningsgrunnlagPeriodeAndelDto(FastsettBeregningsgrunnlagAndelDto fastsettBeregningsgrunnlagAndelDto) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettBeregningsgrunnlagAndelDto(
            mapTilRedigerbarAndelDto(fastsettBeregningsgrunnlagAndelDto),
            mapTilFastsatteVerdier(fastsettBeregningsgrunnlagAndelDto.getFastsatteVerdier()),
            fastsettBeregningsgrunnlagAndelDto.getForrigeInntektskategori() == null ? null : Inntektskategori.fraKode(fastsettBeregningsgrunnlagAndelDto.getForrigeInntektskategori().getKode()),
            fastsettBeregningsgrunnlagAndelDto.getForrigeRefusjonPrÅr(),
            fastsettBeregningsgrunnlagAndelDto.getForrigeArbeidsinntektPrÅr());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelFastsatteVerdierDto mapTilFordelFastsatteVerdier(
        FordelFastsatteVerdierDto fastsatteVerdier) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelFastsatteVerdierDto(
            fastsatteVerdier.getRefusjonPrÅr(),
            null,
            Inntektskategori.fraKode(fastsatteVerdier.getInntektskategori().getKode()),
            fastsatteVerdier.getFastsattÅrsbeløpInklNaturalytelse()
        );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsatteVerdierDto mapTilFastsatteVerdier(FastsatteVerdierDto fastsatteVerdier) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsatteVerdierDto(
            fastsatteVerdier.getFastsattBeløp(),
            Inntektskategori.fraKode(fastsatteVerdier.getInntektskategori().getKode()),
            false);
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelRedigerbarAndelDto mapTilFordelRedigerbarAndelDto(
        FordelRedigerbarAndelDto redigerbarAndel) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fordeling.FordelRedigerbarAndelDto(
            redigerbarAndel.getAndelsnr(),
            redigerbarAndel.getArbeidsgiverId(),
            redigerbarAndel.getArbeidsforholdId().getReferanse(),
            redigerbarAndel.getNyAndel(),
            redigerbarAndel.getKilde() == null ? AndelKilde.PROSESS_START : Arrays.stream(AndelKilde.values()).filter(v -> v.getKode().equals(redigerbarAndel.getKilde().getKode())).findFirst().orElse(AndelKilde.PROSESS_START)
            );
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.RedigerbarAndelDto mapTilRedigerbarAndelDto(RedigerbarAndelDto redigerbarAndel) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.RedigerbarAndelDto(
            redigerbarAndel.getAndelsnr(),
            redigerbarAndel.getLagtTilAvSaksbehandler());
    }

    private static no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettInntektForArbeidUnderAAPDto mapFastsettInntektForArbeidUnderAAP(
        FastsettInntektForArbeidUnderAAPDto fastsettInntektForArbeidUnderAAP) {
        return new no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FastsettInntektForArbeidUnderAAPDto(fastsettInntektForArbeidUnderAAP.getFastsattPrMnd());
    }
}
