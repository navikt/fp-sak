package no.nav.foreldrepenger.domene.mappers.fra_entitet_til_domene;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.BesteberegningInntektEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegningMånedsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegninggrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitet;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyring;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringer;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonOverstyring;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonOverstyringer;
import no.nav.foreldrepenger.domene.modell.BeregningRefusjonPeriode;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BesteberegningGrunnlag;
import no.nav.foreldrepenger.domene.modell.BesteberegningInntekt;
import no.nav.foreldrepenger.domene.modell.BesteberegningMånedsgrunnlag;
import no.nav.foreldrepenger.domene.modell.FaktaAggregat;
import no.nav.foreldrepenger.domene.modell.FaktaAktør;
import no.nav.foreldrepenger.domene.modell.FaktaArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.SammenligningsgrunnlagPrStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaVurderingKilde;
import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.modell.kodeverk.SammenligningsgrunnlagType;
import no.nav.foreldrepenger.domene.modell.typer.FaktaVurdering;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class FraEntitetTilBehandlingsmodellMapper {

    private FraEntitetTilBehandlingsmodellMapper() {
    }

    public static BeregningsgrunnlagGrunnlag mapBeregningsgrunnlagGrunnlag(BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet) {
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(
                grunnlagEntitet.getBeregningsgrunnlag().map(FraEntitetTilBehandlingsmodellMapper::mapBeregningsgrunnlag).orElse(null))
            .medRegisterAktiviteter(grunnlagEntitet.getRegisterAktiviteter() == null ? null : mapBeregningAktivitetAggregat(grunnlagEntitet.getRegisterAktiviteter()))
            .medSaksbehandletAktiviteter(
                grunnlagEntitet.getSaksbehandletAktiviteter().map(FraEntitetTilBehandlingsmodellMapper::mapBeregningAktivitetAggregat).orElse(null))
            .medOverstyring(
                grunnlagEntitet.getOverstyring().map(FraEntitetTilBehandlingsmodellMapper::mapBeregningAktivitetOverstyringer).orElse(null))
            .medRefusjonOverstyring(grunnlagEntitet.getRefusjonOverstyringer().map(FraEntitetTilBehandlingsmodellMapper::mapRefusjonOverstyringer).orElse(null))
            .medFakta(grunnlagEntitet.getBeregningsgrunnlag()
                .flatMap(FraEntitetTilBehandlingsmodellMapper::mapFaktaAggregat).orElse(null))
            .build(grunnlagEntitet.getBeregningsgrunnlagTilstand());
    }

    private static BeregningRefusjonOverstyringer mapRefusjonOverstyringer(BeregningRefusjonOverstyringerEntitet refusjonOverstyringAggregat) {
        var builder = BeregningRefusjonOverstyringer.builder();
        refusjonOverstyringAggregat.getRefusjonOverstyringer().stream().map(FraEntitetTilBehandlingsmodellMapper::mapRefusjonOverstyring).forEach(builder::leggTilOverstyring);
        return builder.build();
    }

    private static BeregningRefusjonOverstyring mapRefusjonOverstyring(BeregningRefusjonOverstyringEntitet refusjonOverstyring) {
        var perioder = refusjonOverstyring.getRefusjonPerioder()
            .stream()
            .map(r -> new BeregningRefusjonPeriode(r.getArbeidsforholdRef(), r.getStartdatoRefusjon()))
            .toList();
        return new BeregningRefusjonOverstyring(refusjonOverstyring.getArbeidsgiver(), refusjonOverstyring.getFørsteMuligeRefusjonFom().orElse(null),
            Boolean.TRUE.equals(refusjonOverstyring.getErFristUtvidet()), perioder);
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringer mapBeregningAktivitetOverstyringer(
        BeregningAktivitetOverstyringerEntitet overstyringer) {
        var builder = BeregningAktivitetOverstyringer.builder();
        overstyringer.getOverstyringer()
            .stream()
            .map(FraEntitetTilBehandlingsmodellMapper::mapAktivitetOverstyring)
            .sorted(Comparator.comparing(BeregningAktivitetOverstyring::getOpptjeningAktivitetType)
                .thenComparing(a -> a.getPeriode().getFomDato())
                .thenComparing(a -> a.getPeriode().getTomDato())
                .thenComparing(a -> a.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(a -> a.getArbeidsforholdRef() == null ? null : a.getArbeidsforholdRef().getReferanse(), Comparator.nullsLast(Comparator.naturalOrder())))
            .forEach(builder::leggTilOverstyring);
        return builder.build();
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyring mapAktivitetOverstyring(BeregningAktivitetOverstyringEntitet beregningAktivitetOverstyringEntitet) {
        return no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyring.builder()
            .medArbeidsgiver(beregningAktivitetOverstyringEntitet.getArbeidsgiver().orElse(null))
            .medArbeidsforholdRef(beregningAktivitetOverstyringEntitet.getArbeidsforholdRef() == null ? InternArbeidsforholdRef.nullRef() : beregningAktivitetOverstyringEntitet.getArbeidsforholdRef())
            .medHandling(beregningAktivitetOverstyringEntitet.getHandling())
            .medOpptjeningAktivitetType(beregningAktivitetOverstyringEntitet.getOpptjeningAktivitetType())
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(beregningAktivitetOverstyringEntitet.getPeriode().getFomDato(),
                beregningAktivitetOverstyringEntitet.getPeriode().getTomDato()))
            .build();
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat mapBeregningAktivitetAggregat(BeregningAktivitetAggregatEntitet registerAktiviteter) {
        var builder = BeregningAktivitetAggregat.builder()
            .medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        registerAktiviteter.getBeregningAktiviteter()
            .stream()
            .map(FraEntitetTilBehandlingsmodellMapper::mapBeregningAktivitet)
            .sorted(Comparator.comparing(BeregningAktivitet::getOpptjeningAktivitetType)
                .thenComparing(a -> a.getPeriode().getFomDato())
                .thenComparing(a -> a.getPeriode().getTomDato())
                .thenComparing(a -> a.getArbeidsgiver() == null ? null : a.getArbeidsgiver().getIdentifikator(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(a -> a.getArbeidsforholdRef() == null ? null : a.getArbeidsforholdRef().getReferanse(), Comparator.nullsLast(Comparator.naturalOrder())))
            .forEach(builder::leggTilAktivitet);
        return builder.build();
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningAktivitet mapBeregningAktivitet(BeregningAktivitetEntitet beregningAktivitetDto) {
        return no.nav.foreldrepenger.domene.modell.BeregningAktivitet.builder()
            .medArbeidsforholdRef(beregningAktivitetDto.getArbeidsforholdRef())
            .medArbeidsgiver(beregningAktivitetDto.getArbeidsgiver())
            .medOpptjeningAktivitetType(beregningAktivitetDto.getOpptjeningAktivitetType())
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(beregningAktivitetDto.getPeriode().getFomDato(),
                beregningAktivitetDto.getPeriode().getTomDato()))
            .build();
    }

    public static Beregningsgrunnlag mapBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlagDto) {
        var builder = Beregningsgrunnlag.builder()
            .medOverstyring(beregningsgrunnlagDto.isOverstyrt())
            .medSkjæringstidspunkt(beregningsgrunnlagDto.getSkjæringstidspunkt())
            .medGrunnbeløp(beregningsgrunnlagDto.getGrunnbeløp());


        // Aktivitetstatuser
        beregningsgrunnlagDto.getAktivitetStatuser().stream()
            .sorted(Comparator.comparing(no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus::getAktivitetStatus))
            .forEach(aktivitetStatus -> builder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(aktivitetStatus.getAktivitetStatus()).medHjemmel(aktivitetStatus.getHjemmel()).build()));

        // Besteberegning
        beregningsgrunnlagDto.getBesteberegninggrunnlag().ifPresent(bb -> builder.medBesteberegningsgrunnlag(mapBesteberegning(bb)));

        // Faktatilfeller
        if (beregningsgrunnlagDto.getFaktaOmBeregningTilfeller() != null) {
            beregningsgrunnlagDto.getFaktaOmBeregningTilfeller().stream().sorted(Comparator.comparing(FaktaOmBeregningTilfelle::getKode)).forEach(builder::leggTilFaktaOmBeregningTilfelle);
        }

        // Beregningsgrunnlagperioder
        mapPerioder(beregningsgrunnlagDto.getBeregningsgrunnlagPerioder()).stream().sorted(Comparator.comparing(
            BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom)).forEach(builder::leggTilBeregningsgrunnlagPeriode);

        // Sammenligningsgrunnlag
        beregningsgrunnlagDto.getSammenligningsgrunnlag().ifPresentOrElse(sg -> builder.leggTilSammenligningsgrunnlagPrStatus(mapSammenligningsgrunnlagTilNyModell(sg, beregningsgrunnlagDto.getAktivitetStatuser())),
            () -> beregningsgrunnlagDto.getSammenligningsgrunnlagPrStatusListe().stream().map(FraEntitetTilBehandlingsmodellMapper::mapSammenligningsgrunnlagPrStatus).forEach(builder::leggTilSammenligningsgrunnlagPrStatus));

        return builder.build();
    }

    private static SammenligningsgrunnlagPrStatus mapSammenligningsgrunnlagPrStatus(no.nav.foreldrepenger.domene.entiteter.SammenligningsgrunnlagPrStatus sammenligningsgrunnlag) {
        return SammenligningsgrunnlagPrStatus.builder()
            .medAvvikPromille(sammenligningsgrunnlag.getAvvikPromille().longValue())
            .medSammenligningsperiode(sammenligningsgrunnlag.getSammenligningsperiodeFom(), sammenligningsgrunnlag.getSammenligningsperiodeTom())
            .medRapportertPrÅr(sammenligningsgrunnlag.getRapportertPrÅr())
            .medSammenligningsgrunnlagType(sammenligningsgrunnlag.getSammenligningsgrunnlagType())
            .build();
    }

    private static SammenligningsgrunnlagPrStatus mapSammenligningsgrunnlagTilNyModell(Sammenligningsgrunnlag sammenligningsgrunnlag,
                                                                                       List<no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus> aktivitetStatuser) {
        return SammenligningsgrunnlagPrStatus.builder()
            .medAvvikPromille(sammenligningsgrunnlag.getAvvikPromille().longValue())
            .medSammenligningsperiode(sammenligningsgrunnlag.getSammenligningsperiodeFom(), sammenligningsgrunnlag.getSammenligningsperiodeTom())
            .medRapportertPrÅr(sammenligningsgrunnlag.getRapportertPrÅr())
            .medSammenligningsgrunnlagType(utledSammenligningsgrunnlagType(aktivitetStatuser))
            .build();
    }

    private static SammenligningsgrunnlagType utledSammenligningsgrunnlagType(List<no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus> aktivitetStatuser) {
        // For å få så nøyaktige data som mulig mapper vi til den typen sammenligningsgrunnlaget ville hatt med ny modell. Så lenge det finnes en SN status er dette et SN grunnlag.
        var finnesSNStatus = aktivitetStatuser.stream().anyMatch(as -> as.getAktivitetStatus().erSelvstendigNæringsdrivende());
        return finnesSNStatus ? SammenligningsgrunnlagType.SAMMENLIGNING_SN : SammenligningsgrunnlagType.SAMMENLIGNING_AT_FL;
    }

    private static BesteberegningGrunnlag mapBesteberegning(BesteberegninggrunnlagEntitet besteberegninggrunnlagEntitet) {
        var builder = BesteberegningGrunnlag.ny();
        besteberegninggrunnlagEntitet.getAvvik().ifPresent(builder::medAvvik);
        besteberegninggrunnlagEntitet.getSeksBesteMåneder().stream().sorted(Comparator.comparing(bbp -> bbp.getPeriode().getFomDato())).forEach(bbMåned -> builder.leggTilMånedsgrunnlag(mapBesteMåned(bbMåned)));
        return builder.build();
    }

    private static BesteberegningMånedsgrunnlag mapBesteMåned(BesteberegningMånedsgrunnlagEntitet bbMåned) {
        var builder = BesteberegningMånedsgrunnlag.ny()
            .medPeriode(bbMåned.getPeriode().getFomDato(), bbMåned.getPeriode().getTomDato());
        bbMåned.getInntekter().forEach(bbInntekt -> builder.leggTilInntekt(mapBesteInntekt(bbInntekt)));
        return builder.build();
    }

    private static BesteberegningInntekt mapBesteInntekt(BesteberegningInntektEntitet bbInntekt) {
        return BesteberegningInntekt.ny()
            .medInntekt(bbInntekt.getInntekt())
            .medArbeidsgiver(bbInntekt.getArbeidsgiver())
            .medArbeidsforholdRef(bbInntekt.getArbeidsforholdRef())
            .medOpptjeningAktivitetType(bbInntekt.getOpptjeningAktivitetType())
            .build();
    }

    private static List<BeregningsgrunnlagPeriode> mapPerioder(List<no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder) {
        return beregningsgrunnlagPerioder.stream().map(FraEntitetTilBehandlingsmodellMapper::mapPeriode).toList();
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode mapPeriode(no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode beregningsgrunnlagPeriodeDto) {
        var periodeBuilder = no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeDto.getPeriode().getFomDato(),
                beregningsgrunnlagPeriodeDto.getPeriode().getTomDato())
            .medAvkortetPrÅr(beregningsgrunnlagPeriodeDto.getAvkortetPrÅr())
            .medBruttoPrÅr(beregningsgrunnlagPeriodeDto.getBruttoPrÅr())
            .medRedusertPrÅr(beregningsgrunnlagPeriodeDto.getRedusertPrÅr())
            .medDagsats(beregningsgrunnlagPeriodeDto.getDagsats());
        beregningsgrunnlagPeriodeDto.getPeriodeÅrsaker().stream().sorted(Comparator.comparing(PeriodeÅrsak::getKode)).forEach(periodeBuilder::leggTilPeriodeÅrsak);
        mapAndeler(beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList()).stream().sorted(Comparator.comparing(
            no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel::getAndelsnr)).forEach(
            periodeBuilder::leggTilBeregningsgrunnlagPrStatusOgAndel);
        return periodeBuilder.build();
    }

    private static List<no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel> mapAndeler(List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList) {
        return beregningsgrunnlagPrStatusOgAndelList.stream().map(FraEntitetTilBehandlingsmodellMapper::mapAndel).toList();
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel mapAndel(BeregningsgrunnlagPrStatusOgAndel andelEntitet) {
        var builder = no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(andelEntitet.getAktivitetStatus())
            .medAndelsnr(andelEntitet.getAndelsnr())
            .medArbforholdType(andelEntitet.getArbeidsforholdType())
            .medAvkortetBrukersAndelPrÅr(andelEntitet.getAvkortetBrukersAndelPrÅr())
            .medAvkortetPrÅr(andelEntitet.getAvkortetPrÅr())
            .medAvkortetRefusjonPrÅr(andelEntitet.getAvkortetRefusjonPrÅr())
            .medBeregnetPrÅr(andelEntitet.getBeregnetPrÅr())
            .medBesteberegnetPrÅr(andelEntitet.getBesteberegningPrÅr())
            .medBruttoPrÅr(andelEntitet.getBruttoPrÅr())
            .medDagsatsBruker(andelEntitet.getDagsatsBruker())
            .medDagsatsArbeidsgiver(andelEntitet.getDagsatsArbeidsgiver())
            .medKilde(andelEntitet.getKilde())
            .medBeregningsperiode(andelEntitet.getBeregningsperiodeFom(),
                andelEntitet.getBeregningsperiodeTom())
            .medFastsattAvSaksbehandler(andelEntitet.getFastsattAvSaksbehandler())
            .medFordeltPrÅr(andelEntitet.getFordeltPrÅr())
            .medManueltFordeltPrÅr(andelEntitet.getManueltFordeltPrÅr())
            .medInntektskategori(andelEntitet.getGjeldendeInntektskategori())
            .medLagtTilAvSaksbehandler(andelEntitet.erLagtTilAvSaksbehandler())
            .medMaksimalRefusjonPrÅr(andelEntitet.getMaksimalRefusjonPrÅr())
            .medOrginalDagsatsFraTilstøtendeYtelse(andelEntitet.getOrginalDagsatsFraTilstøtendeYtelse())
            .medOverstyrtPrÅr(andelEntitet.getOverstyrtPrÅr())
            .medRedusertBrukersAndelPrÅr(andelEntitet.getRedusertBrukersAndelPrÅr())
            .medRedusertPrÅr(andelEntitet.getRedusertPrÅr())
            .medRedusertRefusjonPrÅr(andelEntitet.getRedusertRefusjonPrÅr())
            .medÅrsbeløpFraTilstøtendeYtelse(andelEntitet.getÅrsbeløpFraTilstøtendeYtelse() == null
                ? null
                : andelEntitet.getÅrsbeløpFraTilstøtendeYtelse().getVerdi());

        andelEntitet.getBgAndelArbeidsforhold().ifPresent(bga -> builder.medBGAndelArbeidsforhold(
            FraEntitetTilBehandlingsmodellMapper.mapBgAndelArbeidsforhold(andelEntitet.getBgAndelArbeidsforhold().get())));

        if (andelEntitet.getPgiSnitt() != null) {
            builder.medPgi(andelEntitet.getPgiSnitt(),
                List.of(andelEntitet.getPgi1(), andelEntitet.getPgi2(),
                    andelEntitet.getPgi3()));
        }
        return builder.build();
    }

    private static no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold.Builder mapBgAndelArbeidsforhold(BGAndelArbeidsforhold bgAndelArbeidsforhold) {
        return no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold.builder()
            .medArbeidsforholdRef(bgAndelArbeidsforhold.getArbeidsforholdRef())
            .medArbeidsgiver(bgAndelArbeidsforhold.getArbeidsgiver())
            .medRefusjonskravPrÅr(bgAndelArbeidsforhold.getGjeldendeRefusjon())
            .medArbeidsperiodeFom(bgAndelArbeidsforhold.getArbeidsperiodeFom())
            .medArbeidsperiodeTom(bgAndelArbeidsforhold.getArbeidsperiodeTom().orElse(null))
            .medNaturalytelseBortfaltPrÅr(bgAndelArbeidsforhold.getNaturalytelseBortfaltPrÅr().orElse(null))
            .medNaturalytelseTilkommetPrÅr(bgAndelArbeidsforhold.getNaturalytelseTilkommetPrÅr().orElse(null));
    }

    public static Optional<FaktaAggregat> mapFaktaAggregat(BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        // I fakta om beregning settes alle faktaavklaringer på første periode og vi kan derfor bruke denne til å hente ut avklart fakta
        // Enkelte eldre grunnlag har ikke perioder (f.eks i OPPRETTET tilstand)
        if (beregningsgrunnlagEntitet.getBeregningsgrunnlagPerioder().isEmpty()) {
            return Optional.empty();
        }
        var førstePeriode = beregningsgrunnlagEntitet.getBeregningsgrunnlagPerioder().get(0);
        var andeler = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        var faktaAggregatBuilder = FaktaAggregat.builder();
        mapFaktaArbeidsforhold(andeler).forEach(faktaAggregatBuilder::erstattEksisterendeEllerLeggTil);
        mapFaktaAktør(andeler, beregningsgrunnlagEntitet.getFaktaOmBeregningTilfeller()).ifPresent(faktaAggregatBuilder::medFaktaAktør);
        return faktaAggregatBuilder.manglerFakta() ? Optional.empty() : Optional.of(faktaAggregatBuilder.build());
    }

    private static List<FaktaArbeidsforhold> mapFaktaArbeidsforhold(List<BeregningsgrunnlagPrStatusOgAndel> andeler) {
        return andeler.stream().filter(a -> a.getAktivitetStatus().erArbeidstaker()).filter(a -> a.getArbeidsgiver().isPresent()).map(a -> {
            var builder = new FaktaArbeidsforhold.Builder(a.getArbeidsgiver().get(),
                a.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()));
            a.getBgAndelArbeidsforhold()
                .map(BGAndelArbeidsforhold::getErTidsbegrensetArbeidsforhold)
                .ifPresent(fakta -> builder.medErTidsbegrenset(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
            a.mottarYtelse().ifPresent(fakta -> builder.medHarMottattYtelse(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
            a.getBgAndelArbeidsforhold()
                .map(BGAndelArbeidsforhold::erLønnsendringIBeregningsperioden)
                .ifPresent(fakta -> builder.medHarLønnsendringIBeregningsperioden(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
            return builder.manglerFakta() ? null : builder.build();
        }).filter(Objects::nonNull).toList();
    }

    private static Optional<FaktaAktør> mapFaktaAktør(List<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                                      List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        var faktaAktørBuilder = FaktaAktør.builder();
        mapMilitær(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapEtterlønnSluttpakke(faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapMottarFLYtelse(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapErNyIArbeidslivetSN(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapSkalBesteberegnes(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapErNyoppstartetFL(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        return faktaAktørBuilder.erUgyldig() ? Optional.empty() : Optional.of(faktaAktørBuilder.build());
    }

    private static void mapMilitær(List<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                   List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller,
                                   FaktaAktør.Builder faktaAktørBuilder) {
        var harVurdertMilitær = faktaOmBeregningTilfeller.stream()
            .anyMatch(tilfelle -> tilfelle.equals(FaktaOmBeregningTilfelle.VURDER_MILITÆR_SIVILTJENESTE));
        var harMilitær = andeler.stream().anyMatch(a -> a.getAktivitetStatus().equals(AktivitetStatus.MILITÆR_ELLER_SIVIL));
        if (harVurdertMilitær) {
            faktaAktørBuilder.medErMilitærSiviltjeneste(new FaktaVurdering(harMilitær, FaktaVurderingKilde.SAKSBEHANDLER));
        }
    }

    private static void mapErNyoppstartetFL(List<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                            List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller,
                                            FaktaAktør.Builder faktaAktørBuilder) {
        var harVurdertNyoppstartetFL = faktaOmBeregningTilfeller.stream()
            .anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL));
        if (harVurdertNyoppstartetFL) {
            andeler.stream()
                .filter(a -> a.getAktivitetStatus().erFrilanser() && a.erNyoppstartet().isPresent())
                .findFirst()
                .map(BeregningsgrunnlagPrStatusOgAndel::erNyoppstartet)
                .map(Optional::get)
                .ifPresent(fakta -> faktaAktørBuilder.medErNyoppstartetFL(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
        }
    }

    private static void mapSkalBesteberegnes(List<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                             List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller,
                                             FaktaAktør.Builder faktaAktørBuilder) {
        var harVurdertBesteberegning = faktaOmBeregningTilfeller.stream()
            .anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING));
        if (harVurdertBesteberegning) {
            var harFastsattBesteberegning = andeler.stream().anyMatch(a -> a.getBesteberegningPrÅr() != null);
            faktaAktørBuilder.medSkalBesteberegnes(new FaktaVurdering(harFastsattBesteberegning, FaktaVurderingKilde.SAKSBEHANDLER));
        }
    }

    private static void mapErNyIArbeidslivetSN(List<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                               List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller,
                                               FaktaAktør.Builder faktaAktørBuilder) {
        var harVurdertNyIArbeidslivetSN = faktaOmBeregningTilfeller.stream()
            .anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.VURDER_SN_NY_I_ARBEIDSLIVET));
        if (harVurdertNyIArbeidslivetSN) {
            andeler.stream()
                .filter(a -> a.getAktivitetStatus().erSelvstendigNæringsdrivende())
                .findFirst()
                .map(BeregningsgrunnlagPrStatusOgAndel::getNyIArbeidslivet)
                .ifPresent(fakta -> faktaAktørBuilder.medErNyIArbeidslivetSN(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
        }
    }

    private static void mapMottarFLYtelse(List<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                          List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller,
                                          FaktaAktør.Builder faktaAktørBuilder) {
        var harVurdertMottarYtelse = faktaOmBeregningTilfeller.stream()
            .anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE));
        if (harVurdertMottarYtelse) {
            andeler.stream()
                .filter(a -> a.getAktivitetStatus().erFrilanser() && a.mottarYtelse().isPresent())
                .findFirst()
                .map(BeregningsgrunnlagPrStatusOgAndel::mottarYtelse)
                .map(Optional::get)
                .ifPresent(fakta -> faktaAktørBuilder.medHarFLMottattYtelse(new FaktaVurdering(fakta, FaktaVurderingKilde.SAKSBEHANDLER)));
        }
    }

    private static void mapEtterlønnSluttpakke(List<no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller,
                                               FaktaAktør.Builder faktaAktørBuilder) {
        var harVurdertEtterlønnSluttpakke = faktaOmBeregningTilfeller.stream()
            .anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE));
        var harEtterlønnSlutpakke = faktaOmBeregningTilfeller.stream()
            .anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle.FASTSETT_ETTERLØNN_SLUTTPAKKE));
        if (harVurdertEtterlønnSluttpakke) {
            faktaAktørBuilder.medMottarEtterlønnSluttpakke(new FaktaVurdering(harEtterlønnSlutpakke, FaktaVurderingKilde.SAKSBEHANDLER));
        }
    }
}
