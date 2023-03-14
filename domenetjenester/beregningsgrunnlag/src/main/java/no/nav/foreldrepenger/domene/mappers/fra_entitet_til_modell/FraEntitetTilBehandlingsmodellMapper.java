package no.nav.foreldrepenger.domene.mappers.fra_entitet_til_modell;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.BesteberegningInntektEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegningMånedsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegninggrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringer;
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
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaVurderingKilde;
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
            .medRegisterAktiviteter(mapBeregningAktivitetAggregat(grunnlagEntitet.getRegisterAktiviteter()))
            .medSaksbehandletAktiviteter(
                grunnlagEntitet.getSaksbehandletAktiviteter().map(FraEntitetTilBehandlingsmodellMapper::mapBeregningAktivitetAggregat).orElse(null))
            .medOverstyring(
                grunnlagEntitet.getOverstyring().map(FraEntitetTilBehandlingsmodellMapper::mapBeregningAktivitetOverstyringer).orElse(null))
            .medFakta(grunnlagEntitet.getBeregningsgrunnlag()
                .flatMap(FraEntitetTilBehandlingsmodellMapper::mapFaktaAggregat).orElse(null))
            .build(grunnlagEntitet.getBeregningsgrunnlagTilstand());
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyringer mapBeregningAktivitetOverstyringer(
        BeregningAktivitetOverstyringerEntitet overstyringer) {
        var builder = BeregningAktivitetOverstyringer.builder();
        overstyringer.getOverstyringer()
            .stream()
            .map(FraEntitetTilBehandlingsmodellMapper::mapAktivitetOverstyring)
            .forEach(builder::leggTilOverstyring);
        return builder.build();
    }

    private static no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyring mapAktivitetOverstyring(BeregningAktivitetOverstyringEntitet beregningAktivitetOverstyringEntitet) {
        return no.nav.foreldrepenger.domene.modell.BeregningAktivitetOverstyring.builder()
            .medArbeidsgiver(beregningAktivitetOverstyringEntitet.getArbeidsgiver().orElse(null))
            .medArbeidsforholdRef(beregningAktivitetOverstyringEntitet.getArbeidsforholdRef())
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


        beregningsgrunnlagDto.getAktivitetStatuser()
            .forEach(aktivitetStatus -> builder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(aktivitetStatus.getAktivitetStatus()).medHjemmel(aktivitetStatus.getHjemmel()).build()));
        beregningsgrunnlagDto.getBesteberegninggrunnlag().ifPresent(bb -> builder.medBesteberegningsgrunnlag(mapBesteberegning(bb)));
        if (beregningsgrunnlagDto.getFaktaOmBeregningTilfeller() != null) {
            builder.leggTilFaktaOmBeregningTilfeller(beregningsgrunnlagDto.getFaktaOmBeregningTilfeller());
        }
        mapPerioder(beregningsgrunnlagDto.getBeregningsgrunnlagPerioder()).forEach(builder::leggTilBeregningsgrunnlagPeriode);
        beregningsgrunnlagDto.getSammenligningsgrunnlag().ifPresent(sg -> builder.medSammenligningsgrunnlag(mapSammenligningsgrunnlag(sg)));
        return builder.build();
    }

    private static BesteberegningGrunnlag mapBesteberegning(BesteberegninggrunnlagEntitet besteberegninggrunnlagEntitet) {
        var builder = BesteberegningGrunnlag.ny();
        besteberegninggrunnlagEntitet.getAvvik().ifPresent(builder::medAvvik);
        besteberegninggrunnlagEntitet.getSeksBesteMåneder().forEach(bbMåned -> builder.leggTilMånedsgrunnlag(mapBesteMåned(bbMåned)));
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
            .medBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPeriodeFom(),
                beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPeriodeTom())
            .medAvkortetPrÅr(beregningsgrunnlagPeriodeDto.getAvkortetPrÅr())
            .medBruttoPrÅr(beregningsgrunnlagPeriodeDto.getBruttoPrÅr())
            .medRedusertPrÅr(beregningsgrunnlagPeriodeDto.getRedusertPrÅr());
        mapAndeler(beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList()).forEach(
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
            .medInntektskategori(andelEntitet.getInntektskategori())
            .medInntektskategoriAutomatiskFordeling(andelEntitet.getInntektskategoriAutomatiskFordeling())
            .medInntektskategoriManuellFordeling(andelEntitet.getInntektskategoriManuellFordeling())
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

    private static no.nav.foreldrepenger.domene.modell.Sammenligningsgrunnlag mapSammenligningsgrunnlag(Sammenligningsgrunnlag sammenligningsgrunnlag) {
        return no.nav.foreldrepenger.domene.modell.Sammenligningsgrunnlag.builder()
            .medSammenligningsperiode(sammenligningsgrunnlag.getSammenligningsperiodeFom(), sammenligningsgrunnlag.getSammenligningsperiodeTom())
            .medRapportertPrÅr(sammenligningsgrunnlag.getRapportertPrÅr())
            .medAvvikPromille(sammenligningsgrunnlag.getAvvikPromille().longValue()).build();
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
        mapEtterlønnSluttpakke(faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapMottarFLYtelse(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapErNyIArbeidslivetSN(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapSkalBesteberegnes(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapErNyoppstartetFL(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        return faktaAktørBuilder.erUgyldig() ? Optional.empty() : Optional.of(faktaAktørBuilder.build());
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
