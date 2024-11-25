package no.nav.foreldrepenger.domene.migrering;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BGAndelArbeidsforholdMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BaseMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningAktivitetAggregatMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningAktivitetMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningAktivitetOverstyringMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningAktivitetOverstyringerMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningRefusjonOverstyringMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningRefusjonOverstyringerMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningRefusjonPeriodeMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningsgrunnlagAktivitetStatusMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningsgrunnlagGrunnlagMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningsgrunnlagMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningsgrunnlagPeriodeMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningsgrunnlagPrStatusOgAndelMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BesteberegningInntektMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BesteberegningMånedsgrunnlagMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BesteberegninggrunnlagMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.FaktaAggregatMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.FaktaAktørMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.FaktaArbeidsforholdMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.FaktaVurderingMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.SammenligningsgrunnlagPrStatusMigreringDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Beløp;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.kodeverk.FaktaVurderingKilde;
import no.nav.folketrygdloven.kalkulus.kodeverk.SammenligningsgrunnlagType;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonPeriodeEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagArbeidstakerAndel;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagFrilansAndel;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.BesteberegningInntektEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegningMånedsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegninggrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.entiteter.SammenligningsgrunnlagPrStatus;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.KodeverkTilKalkulusMapper;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class BeregningMigreringMapper {
    private BeregningMigreringMapper() {
        // Skjuler default
    }

    public static BeregningsgrunnlagGrunnlagMigreringDto map(BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        return mapGrunnlag(grunnlag);
    }

    private static BeregningsgrunnlagGrunnlagMigreringDto mapGrunnlag(BeregningsgrunnlagGrunnlagEntitet entitet) {
        var aktivitetOverstyringer = entitet.getOverstyring().map(BeregningMigreringMapper::mapAktivitetOverstyringer).orElse(null);
        var saksbehandletAktiviteter = entitet.getSaksbehandletAktiviteter().map(BeregningMigreringMapper::mapAktiviteter).orElse(null);
        var registerAktiviteter = entitet.getRegisterAktiviteter() == null ? null : mapAktiviteter(entitet.getRegisterAktiviteter());
        var refusjonOverstyringer = entitet.getRefusjonOverstyringer().map(BeregningMigreringMapper::mapRefusjonOverstyringer).orElse(null);
        var faktaAggregat = entitet.getBeregningsgrunnlag().flatMap(BeregningMigreringMapper::mapFaktaAggregat).orElse(null);
        var beregningsgrunnlag = entitet.getBeregningsgrunnlag().map(BeregningMigreringMapper::mapBeregningsgrunnlag).orElse(null);
        var tilstand = KodeverkTilKalkulusMapper.mapBeregningsgrunnlagTilstand(entitet.getBeregningsgrunnlagTilstand());
        var dto = new BeregningsgrunnlagGrunnlagMigreringDto(beregningsgrunnlag, registerAktiviteter, saksbehandletAktiviteter, aktivitetOverstyringer, refusjonOverstyringer, faktaAggregat, tilstand);
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BeregningsgrunnlagMigreringDto mapBeregningsgrunnlag(BeregningsgrunnlagEntitet entitet) {
        var besteberegningGrunnlag = entitet.getBesteberegninggrunnlag().map(BeregningMigreringMapper::mapBesteberegningGrunnlag);
        var faktaTilfeller = entitet.getFaktaOmBeregningTilfeller().stream().map(KodeverkTilKalkulusMapper::mapFaktaBeregningTilfelle).toList();
        var aktivitetStatuser = entitet.getAktivitetStatuser().stream().map(BeregningMigreringMapper::mapTilAktivitetstatus).toList();
        var sammenligningsgrunnlag = mapAlleSammenligningsgrunnlag(entitet);
        var perioder = entitet.getBeregningsgrunnlagPerioder().stream().map(BeregningMigreringMapper::mapBeregningsgrunnlagPeriode).toList();
        var grunnbeløp = entitet.getGrunnbeløp() == null ? null : mapBeløp(entitet.getGrunnbeløp().getVerdi());
        var dto = new BeregningsgrunnlagMigreringDto(entitet.getSkjæringstidspunkt(), aktivitetStatuser, perioder,
            besteberegningGrunnlag.orElse(null), sammenligningsgrunnlag, grunnbeløp, faktaTilfeller, entitet.isOverstyrt());
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BeregningsgrunnlagPeriodeMigreringDto mapBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode entitet) {
        var andeler = entitet.getBeregningsgrunnlagPrStatusOgAndelList().stream().map(BeregningMigreringMapper::mapAndel).toList();
        var periodeårsaker = entitet.getPeriodeÅrsaker().stream().map(KodeverkTilKalkulusMapper::mapPeriodeårsak).toList();
        var periode = mapPeriode(entitet.getPeriode());
        var avkortet = mapBeløp(entitet.getAvkortetPrÅr());
        var redusert = mapBeløp(entitet.getRedusertPrÅr());
        var brutto = mapBeløp(entitet.getBruttoPrÅr());
        var dto = new BeregningsgrunnlagPeriodeMigreringDto(andeler, periode, brutto, avkortet, redusert,
            entitet.getDagsats(), periodeårsaker);
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BeregningsgrunnlagPrStatusOgAndelMigreringDto mapAndel(BeregningsgrunnlagPrStatusOgAndel entitet) {
        var dto = new BeregningsgrunnlagPrStatusOgAndelMigreringDto();

        // Andelsnr og periode
        dto.setAndelsnr(entitet.getAndelsnr());
        dto.setBeregningsperiode(entitet.getBeregningsperiodeFom() == null ? null : mapPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(entitet.getBeregningsperiodeFom(), entitet.getBeregningsperiodeTom())));

        // Inntektskategorier
        dto.setInntektskategori(KodeverkTilKalkulusMapper.mapInntektskategori(entitet.getInntektskategori()));
        dto.setInntektskategoriAutomatiskFordeling(entitet.getInntektskategoriAutomatiskFordeling() == null ? null : KodeverkTilKalkulusMapper.mapInntektskategori(entitet.getInntektskategoriAutomatiskFordeling()));
        dto.setInntektskategoriManuellFordeling(entitet.getInntektskategoriManuellFordeling() == null ? null : KodeverkTilKalkulusMapper.mapInntektskategori(entitet.getInntektskategoriManuellFordeling()));

        // Andre kodeverk
        dto.setAktivitetStatus(KodeverkTilKalkulusMapper.mapAktivitetstatus(entitet.getAktivitetStatus()));
        dto.setArbeidsforholdType(KodeverkTilKalkulusMapper.mapOpptjeningAktivitetType(entitet.getArbeidsforholdType()));
        dto.setKilde(KodeverkTilKalkulusMapper.mapAndelkilde(entitet.getKilde()));

        // PGI
        dto.setPgi1(mapBeløp(entitet.getPgi1()));
        dto.setPgi2(mapBeløp(entitet.getPgi2()));
        dto.setPgi3(mapBeløp(entitet.getPgi3()));
        dto.setPgiSnitt(mapBeløp(entitet.getPgiSnitt()));

        // Avkortet og redusert
        dto.setAvkortetBrukersAndelPrÅr(mapBeløp(entitet.getAvkortetBrukersAndelPrÅr()));
        dto.setAvkortetRefusjonPrÅr(mapBeløp(entitet.getAvkortetRefusjonPrÅr()));
        dto.setAvkortetPrÅr(mapBeløp(entitet.getAvkortetPrÅr()));
        dto.setRedusertBrukersAndelPrÅr(mapBeløp(entitet.getRedusertBrukersAndelPrÅr()));
        dto.setRedusertRefusjonPrÅr(mapBeløp(entitet.getRedusertRefusjonPrÅr()));
        dto.setRedusertPrÅr(mapBeløp(entitet.getRedusertPrÅr()));
        dto.setMaksimalRefusjonPrÅr(mapBeløp(entitet.getMaksimalRefusjonPrÅr()));


        // Dagsatser
        dto.setDagsatsArbeidsgiver(entitet.getDagsatsArbeidsgiver());
        dto.setDagsatsBruker(entitet.getDagsatsBruker());

        // Grunnlag
        dto.setBruttoPrÅr(mapBeløp(entitet.getBruttoPrÅr()));
        dto.setBeregnetPrÅr(mapBeløp(entitet.getBeregnetPrÅr()));
        dto.setOverstyrtPrÅr(mapBeløp(entitet.getOverstyrtPrÅr()));
        dto.setBesteberegningPrÅr(mapBeløp(entitet.getBesteberegningPrÅr()));
        dto.setFordeltPrÅr(mapBeløp(entitet.getFordeltPrÅr()));
        dto.setManueltFordeltPrÅr(mapBeløp(entitet.getManueltFordeltPrÅr()));

        // Div fakta
        dto.setOrginalDagsatsFraTilstøtendeYtelse(entitet.getOrginalDagsatsFraTilstøtendeYtelse());
        dto.setFastsattAvSaksbehandler(entitet.getFastsattAvSaksbehandler());
        dto.setÅrsbeløpFraTilstøtendeYtelse(entitet.getÅrsbeløpFraTilstøtendeYtelse() == null ? null : mapBeløp(entitet.getÅrsbeløpFraTilstøtendeYtelse().getVerdi()));

        // Arbeidsforhold
        entitet.getBgAndelArbeidsforhold().map(BeregningMigreringMapper::mapBgAndelArbeidsforhold).ifPresent(dto::setBgAndelArbeidsforhold);

        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BGAndelArbeidsforholdMigreringDto mapBgAndelArbeidsforhold(BGAndelArbeidsforhold entitet) {
        var dto = new BGAndelArbeidsforholdMigreringDto();

        // Arbeidsinformasjon
        dto.setArbeidsgiver(mapArbeidsgiver(entitet.getArbeidsgiver()));
        dto.setArbeidsforholdRef(mapArbeidsforholdRef(entitet.getArbeidsforholdRef()));
        dto.setArbeidsperiodeFom(entitet.getArbeidsperiodeFom());
        dto.setArbeidsperiodeTom(entitet.getArbeidsperiodeTom().orElse(null));

        // Naturalytelser
        dto.setNaturalytelseBortfaltPrÅr(mapBeløp(entitet.getNaturalytelseBortfaltPrÅr().orElse(null)));
        dto.setNaturalytelseTilkommetPrÅr(mapBeløp(entitet.getNaturalytelseTilkommetPrÅr().orElse(null)));

        // Refusjon
        dto.setRefusjonskravPrÅr(mapBeløp(entitet.getRefusjonskravPrÅr()));
        dto.setSaksbehandletRefusjonPrÅr(mapBeløp(entitet.getSaksbehandletRefusjonPrÅr()));
        dto.setFordeltRefusjonPrÅr(mapBeløp(entitet.getFordeltRefusjonPrÅr()));
        dto.setManueltFordeltRefusjonPrÅr(mapBeløp(entitet.getManueltFordeltRefusjonPrÅr()));

        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static List<SammenligningsgrunnlagPrStatusMigreringDto> mapAlleSammenligningsgrunnlag(BeregningsgrunnlagEntitet entitet) {
        // Vi har satt sammenligningsgrunnlag av ny type, migrerer kun disse
        if (!entitet.getSammenligningsgrunnlagPrStatusListe().isEmpty()) {
            return entitet.getSammenligningsgrunnlagPrStatusListe().stream().map(BeregningMigreringMapper::mapSgPrStatus).toList();
        }
        // Hvis vi har satt gammelt sammenligningsgrunnlag, konverterer dette til den nye typen, ellers tom liste
        return entitet.getSammenligningsgrunnlag().map(sg -> Collections.singletonList(mapGammelSgTilNyModell(sg, entitet))).orElse(List.of());
    }

    private static SammenligningsgrunnlagPrStatusMigreringDto mapGammelSgTilNyModell(Sammenligningsgrunnlag sg, BeregningsgrunnlagEntitet bg) {
        var type = finnSammenligningtypeFraAktivitetstatus(bg);
        var periode = mapPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(sg.getSammenligningsperiodeFom(), sg.getSammenligningsperiodeTom()));
        var rapportertPrÅr = mapBeløp(sg.getRapportertPrÅr());
        var dto = new SammenligningsgrunnlagPrStatusMigreringDto(periode, type, rapportertPrÅr,
            sg.getAvvikPromille());
        settOpprettetOgEndretFelter(sg, dto);
        return dto;
    }

    private static SammenligningsgrunnlagType finnSammenligningtypeFraAktivitetstatus(BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        if (beregningsgrunnlagEntitet.getAktivitetStatuser().stream().anyMatch(st -> st.getAktivitetStatus().erSelvstendigNæringsdrivende())) {
            return SammenligningsgrunnlagType.SAMMENLIGNING_SN;
        } else if (beregningsgrunnlagEntitet.getAktivitetStatuser().stream().anyMatch(st -> st.getAktivitetStatus().erFrilanser()
            || st.getAktivitetStatus().erArbeidstaker())) {
            return SammenligningsgrunnlagType.SAMMENLIGNING_AT_FL;
        }
        throw new IllegalStateException("Klarte ikke utlede sammenligningstype for gammelt grunnlag. Aktivitetstatuser var " + beregningsgrunnlagEntitet.getAktivitetStatuser());
    }


    private static SammenligningsgrunnlagPrStatusMigreringDto mapSgPrStatus(SammenligningsgrunnlagPrStatus entitet) {
        var periode = mapPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(entitet.getSammenligningsperiodeFom(), entitet.getSammenligningsperiodeTom()));
        var rapportertPrÅr = mapBeløp(entitet.getRapportertPrÅr());
        var type = KodeverkTilKalkulusMapper.mapSammenligningsgrunnlagtype(entitet.getSammenligningsgrunnlagType());
        var dto = new SammenligningsgrunnlagPrStatusMigreringDto(periode, type, rapportertPrÅr,
            entitet.getAvvikPromille());
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BeregningsgrunnlagAktivitetStatusMigreringDto mapTilAktivitetstatus(BeregningsgrunnlagAktivitetStatus entitet) {
        var dto = new BeregningsgrunnlagAktivitetStatusMigreringDto(
            KodeverkTilKalkulusMapper.mapAktivitetstatus(entitet.getAktivitetStatus()), KodeverkTilKalkulusMapper.mapHjemmel(entitet.getHjemmel()));
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BesteberegninggrunnlagMigreringDto mapBesteberegningGrunnlag(BesteberegninggrunnlagEntitet entitet) {
        var måneder = entitet.getSeksBesteMåneder().stream().map(BeregningMigreringMapper::mapBesteberegningMåned).collect(Collectors.toSet());
        var avvik = mapBeløp(entitet.getAvvik().orElse(null));
        var dto = new BesteberegninggrunnlagMigreringDto(måneder, avvik);
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BesteberegningMånedsgrunnlagMigreringDto mapBesteberegningMåned(BesteberegningMånedsgrunnlagEntitet entitet) {
        var inntekter = entitet.getInntekter().stream().map(BeregningMigreringMapper::mapBesteberegningInntekt).toList();
        var periode = mapPeriode(entitet.getPeriode());
        var dto = new BesteberegningMånedsgrunnlagMigreringDto(inntekter, periode);
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BesteberegningInntektMigreringDto mapBesteberegningInntekt(BesteberegningInntektEntitet entitet) {
        var ag = mapArbeidsgiver(entitet.getArbeidsgiver());
        var ref = mapArbeidsforholdRef(entitet.getArbeidsforholdRef());
        var beløp = mapBeløp(entitet.getInntekt());
        var dto = new BesteberegningInntektMigreringDto(KodeverkTilKalkulusMapper.mapOpptjeningAktivitetType(entitet.getOpptjeningAktivitetType()), ag, ref, beløp);
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static Beløp mapBeløp(BigDecimal inntekt) {
        return inntekt == null ? null : new Beløp(inntekt);
    }

    private static Optional<FaktaAggregatMigreringDto> mapFaktaAggregat(BeregningsgrunnlagEntitet bg) {
        if (bg.getBeregningsgrunnlagPerioder() == null || bg.getBeregningsgrunnlagPerioder().isEmpty()) {
            return Optional.empty();
        }
        var bgp = bg.getBeregningsgrunnlagPerioder().getFirst();

        sanitycheckBesteberegning(bg);

        var faktaAktørMigreringDto = mapFaktaAktør(bgp.getBeregningsgrunnlagPrStatusOgAndelList(), bg.getFaktaOmBeregningTilfeller());
        var faktaArbeidsforhold = mapAlleFaktaArbeidsforhold(bgp.getBeregningsgrunnlagPrStatusOgAndelList(),
            bg.getFaktaOmBeregningTilfeller());
        if (faktaAktørMigreringDto.isEmpty() && faktaArbeidsforhold.isEmpty()) {
            return Optional.empty();
        }

        // Siden disse entitetene er nye, setter vi opprettet og endret felter til å matche grunnlagsentieten
        faktaAktørMigreringDto.ifPresent(fakta -> settOpprettetOgEndretFelter(bg, fakta));
        faktaArbeidsforhold.forEach(fakta -> settOpprettetOgEndretFelter(bg, fakta));
       var dto = new FaktaAggregatMigreringDto(faktaArbeidsforhold, faktaAktørMigreringDto.orElse(null));
       settOpprettetOgEndretFelter(bg, dto);
        return Optional.of(dto);
    }

    private static List<FaktaArbeidsforholdMigreringDto> mapAlleFaktaArbeidsforhold(List<BeregningsgrunnlagPrStatusOgAndel> andeler, List<FaktaOmBeregningTilfelle> tilfeller) {
        var faktaArbeidsforhold = andeler.stream()
            .filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER) && a.getBgAndelArbeidsforhold().isPresent())
            .map(a -> mapFaktaArbeidsforhold(a, tilfeller))
            .flatMap(Optional::stream)
            .toList();
        return faktaArbeidsforhold;
    }

    private static Optional<FaktaArbeidsforholdMigreringDto> mapFaktaArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel andel, List<FaktaOmBeregningTilfelle> tilfeller) {
        var arbfor = andel.getBgAndelArbeidsforhold()
            .orElseThrow(() -> new IllegalArgumentException("Forventet å finne arbeidsforhold her"));
        var ag = mapArbeidsgiver(arbfor.getArbeidsgiver());
        var ref = mapArbeidsforholdRef(arbfor.getArbeidsforholdRef());
        var erTidsbegrensetVurdering = sjekkOmErTitdsbegrenset(arbfor);
        var harLønnsendringVurdering = sjekkOmHarHattLønnsendring(arbfor);
        var mottarYtelseVurdering = sjekkOmMottarYtelse(andel);
        var arbeidsforholdFakta = new FaktaArbeidsforholdMigreringDto(ag, ref, erTidsbegrensetVurdering.orElse(null),
            mottarYtelseVurdering.orElse(null), harLønnsendringVurdering.orElse(null));

        // Er noe fakta satt?
        if (arbeidsforholdFakta.getHarMottattYtelse() == null && arbeidsforholdFakta.getErTidsbegrenset() == null
            && arbeidsforholdFakta.getHarLønnsendringIBeregningsperioden() == null) {
            return Optional.empty();
        }
        return Optional.of(arbeidsforholdFakta);
    }

    private static Optional<FaktaVurderingMigreringDto> sjekkOmMottarYtelse(BeregningsgrunnlagPrStatusOgAndel andel) {
        var mottarYtelseVurdering = andel.getBeregningsgrunnlagArbeidstakerAndel().map(BeregningsgrunnlagArbeidstakerAndel::getMottarYtelse).orElse(null);
        if (mottarYtelseVurdering == null) {
            return Optional.empty();
        }
        return Optional.of(new FaktaVurderingMigreringDto(mottarYtelseVurdering, FaktaVurderingKilde.SAKSBEHANDLER));
    }

    private static Optional<FaktaVurderingMigreringDto> sjekkOmHarHattLønnsendring(BGAndelArbeidsforhold arbfor) {
        if (arbfor.erLønnsendringIBeregningsperioden() == null) {
            return Optional.empty();
        }
        return Optional.of(new FaktaVurderingMigreringDto(arbfor.erLønnsendringIBeregningsperioden(), FaktaVurderingKilde.SAKSBEHANDLER));
    }

    private static Optional<FaktaVurderingMigreringDto> sjekkOmErTitdsbegrenset(BGAndelArbeidsforhold arbfor) {
        if (arbfor.getErTidsbegrensetArbeidsforhold() == null) {
            return Optional.empty();
        }
        return Optional.of(new FaktaVurderingMigreringDto(arbfor.getErTidsbegrensetArbeidsforhold(), FaktaVurderingKilde.SAKSBEHANDLER));

    }

    private static void sanitycheckBesteberegning(BeregningsgrunnlagEntitet bg) {
        var erAutomatiskBesteberegnet = bg.getBesteberegninggrunnlag().isPresent();
        var harManueltTilfelle = bg.getFaktaOmBeregningTilfeller().stream()
            .anyMatch(t -> t.equals(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING) || t.equals(FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE));
        if (erAutomatiskBesteberegnet && harManueltTilfelle) {
            throw new IllegalStateException("Er både automatisk og manuelt besteberegnet, ugyldig tilstand");
        }
    }

    private static Optional<FaktaAktørMigreringDto> mapFaktaAktør(List<BeregningsgrunnlagPrStatusOgAndel> andelListe, List<FaktaOmBeregningTilfelle> tilfeller) {
        var dto = new FaktaAktørMigreringDto();
        var nyIArbeidslivetVurdering = finnAndel(andelListe, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .map(BeregningsgrunnlagPrStatusOgAndel::getNyIArbeidslivet)
            .map(b -> new FaktaVurderingMigreringDto(b, FaktaVurderingKilde.SAKSBEHANDLER));
        var frilansMottarYtelseVurdering = finnAndel(andelListe, AktivitetStatus.FRILANSER)
            .flatMap(BeregningsgrunnlagPrStatusOgAndel::getBeregningsgrunnlagFrilansAndel)
            .map(BeregningsgrunnlagFrilansAndel::getMottarYtelse)
            .map(b -> new FaktaVurderingMigreringDto(b, FaktaVurderingKilde.SAKSBEHANDLER));
        var frilansNyoppstartetVurdering = finnAndel(andelListe, AktivitetStatus.FRILANSER)
            .flatMap(BeregningsgrunnlagPrStatusOgAndel::getBeregningsgrunnlagFrilansAndel)
            .map(BeregningsgrunnlagFrilansAndel::getNyoppstartet)
            .map(b -> new FaktaVurderingMigreringDto(b, FaktaVurderingKilde.SAKSBEHANDLER));
        var etterlønnSluttpakkeAndeler = andelListe.stream()
            .filter(a -> a.getArbeidsforholdType().equals(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE))
            .toList();
        var etterlønnSluttpakkeFaktaAvklaring = utledFaktaAvklaringEtterlønn(etterlønnSluttpakkeAndeler);
        var skalBesteberegnesVurdering = sjekkOmErBesteberegnetManuelt(tilfeller, andelListe);
        var erMilitærVurdering = sjekkOmErVurdertMilitær(tilfeller, andelListe);

        nyIArbeidslivetVurdering.ifPresent(dto::setErNyIArbeidslivetSN);
        frilansMottarYtelseVurdering.ifPresent(dto::setHarFLMottattYtelse);
        frilansNyoppstartetVurdering.ifPresent(dto::setErNyoppstartetFL);
        etterlønnSluttpakkeFaktaAvklaring.ifPresent(dto::setMottarEtterlønnSluttpakke);
        skalBesteberegnesVurdering.ifPresent(dto::setSkalBesteberegnes);
        erMilitærVurdering.ifPresent(dto::setSkalBeregnesSomMilitær);

        // Sjekk om alle felter er null og returnerer empty hvis sant
        if (dto.getSkalBesteberegnes() == null && dto.getMottarEtterlønnSluttpakke() == null && dto.getSkalBeregnesSomMilitær() == null
            && dto.getHarFLMottattYtelse() == null && dto.getErNyoppstartetFL() == null && dto.getErNyIArbeidslivetSN() == null) {
            return Optional.empty();
        }
        return Optional.of(dto);
    }

    private static Optional<FaktaVurderingMigreringDto> sjekkOmErVurdertMilitær(List<FaktaOmBeregningTilfelle> tilfeller, List<BeregningsgrunnlagPrStatusOgAndel> andelListe) {
        var harMilitærTilfelle = tilfeller.stream().anyMatch(t -> t.equals(FaktaOmBeregningTilfelle.VURDER_MILITÆR_SIVILTJENESTE));
        if (!harMilitærTilfelle) {
            return Optional.empty();
        }
        var harMilitærandel = andelListe.stream().anyMatch(a -> a.getAktivitetStatus().equals(AktivitetStatus.MILITÆR_ELLER_SIVIL));
        return Optional.of(new FaktaVurderingMigreringDto(harMilitærandel, FaktaVurderingKilde.SAKSBEHANDLER));

    }

    private static Optional<FaktaVurderingMigreringDto> sjekkOmErBesteberegnetManuelt(List<FaktaOmBeregningTilfelle> tilfeller, List<BeregningsgrunnlagPrStatusOgAndel> andelListe) {
        // Vi har flere måter å sjekke om det er besteberegnet på siden dette har endret seg litt siden første lansering. Sjekker om det er tvetydighet, og baserer og ellers på om vi har utledet fakta tilfelle
        var harVurderManuellBesteberegningTilfelle = tilfeller.stream().anyMatch(b -> b.equals(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING));

        if (!harVurderManuellBesteberegningTilfelle) {
            return Optional.empty();
        }
        var harManuellBesteberegningtilfelle = tilfeller.stream().anyMatch(b -> b.equals(FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE));
        var harSattManuellBesteberegning = andelListe.stream().anyMatch(a -> a.getBesteberegningPrÅr() != null && a.getBesteberegningPrÅr().compareTo(BigDecimal.ZERO)<0);

        if (!Objects.equals(harManuellBesteberegningtilfelle, harSattManuellBesteberegning)) {
            throw new IllegalStateException("Tvetydig data ved mapping av manuell besteberegning, må undersøkes manuelt");
        }
        var erManueltBesteberegnet = andelListe.stream().anyMatch(a -> a.getBesteberegningPrÅr() != null && a.getBesteberegningPrÅr().compareTo(BigDecimal.ZERO)<0);
        return Optional.of(new FaktaVurderingMigreringDto(erManueltBesteberegnet, FaktaVurderingKilde.SAKSBEHANDLER));
    }

    private static Optional<FaktaVurderingMigreringDto> utledFaktaAvklaringEtterlønn(List<BeregningsgrunnlagPrStatusOgAndel> etterlønnSluttpakkeAndeler) {
        if (etterlønnSluttpakkeAndeler.isEmpty()) {
            return Optional.empty();
        }
        var erManueltFastsattHøyereEnn0 = etterlønnSluttpakkeAndeler.stream()
            .anyMatch(a -> Boolean.TRUE.equals(a.getFastsattAvSaksbehandler()) && a.getBeregnetPrÅr() != null
                && a.getBeregnetPrÅr().compareTo(BigDecimal.ZERO) > 0);
        return erManueltFastsattHøyereEnn0 ?
            Optional.of(new FaktaVurderingMigreringDto(Boolean.TRUE, FaktaVurderingKilde.SAKSBEHANDLER))
            : Optional.of(new FaktaVurderingMigreringDto(Boolean.FALSE, FaktaVurderingKilde.SAKSBEHANDLER));
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndel> finnAndel(List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList, AktivitetStatus aktivitetStatus) {
        return beregningsgrunnlagPrStatusOgAndelList.stream().filter(a -> a.getAktivitetStatus().equals(aktivitetStatus)).findFirst();
    }

    private static BeregningRefusjonOverstyringerMigreringDto mapRefusjonOverstyringer(BeregningRefusjonOverstyringerEntitet entitet) {
        var overstyringer = entitet.getRefusjonOverstyringer().stream().map(BeregningMigreringMapper::mapRefusjonOverstyring).toList();
        var dto = new BeregningRefusjonOverstyringerMigreringDto(overstyringer);
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BeregningRefusjonOverstyringMigreringDto mapRefusjonOverstyring(BeregningRefusjonOverstyringEntitet entitet) {
        var arbeidsgiver = mapArbeidsgiver(entitet.getArbeidsgiver());
        var perioder = entitet.getRefusjonPerioder().stream().map(BeregningMigreringMapper::mapRefusjonsperiode).toList();
        var dto = new BeregningRefusjonOverstyringMigreringDto(arbeidsgiver, entitet.getFørsteMuligeRefusjonFom().orElse(null),
            entitet.getErFristUtvidet(), perioder);
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BeregningRefusjonPeriodeMigreringDto mapRefusjonsperiode(BeregningRefusjonPeriodeEntitet entitet) {
        var ref = mapArbeidsforholdRef(entitet.getArbeidsforholdRef());
        var dto = new BeregningRefusjonPeriodeMigreringDto(ref, entitet.getStartdatoRefusjon());
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BeregningAktivitetAggregatMigreringDto mapAktiviteter(BeregningAktivitetAggregatEntitet entitet) {
        var aktiviteter = entitet.getBeregningAktiviteter().stream().map(BeregningMigreringMapper::mapAktivitet).toList();
        var dto = new BeregningAktivitetAggregatMigreringDto(aktiviteter,
            entitet.getSkjæringstidspunktOpptjening());
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BeregningAktivitetMigreringDto mapAktivitet(BeregningAktivitetEntitet entitet) {
        var periode = mapPeriode(entitet.getPeriode());
        var arbeidsgiver = mapArbeidsgiver(entitet.getArbeidsgiver());
        var arbeidsforholdRef = mapArbeidsforholdRef(entitet.getArbeidsforholdRef());
        var opptjeningAktivitetType = KodeverkTilKalkulusMapper.mapOpptjeningAktivitetType(entitet.getOpptjeningAktivitetType());
        var dto = new BeregningAktivitetMigreringDto(periode, arbeidsgiver, arbeidsforholdRef, opptjeningAktivitetType);
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BeregningAktivitetOverstyringerMigreringDto mapAktivitetOverstyringer(BeregningAktivitetOverstyringerEntitet entitet) {
        var overstyringer = entitet.getOverstyringer().stream().map(BeregningMigreringMapper::mapAktivitetOverstyring).toList();
        var dto = new BeregningAktivitetOverstyringerMigreringDto(overstyringer);
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }

    private static BeregningAktivitetOverstyringMigreringDto mapAktivitetOverstyring(BeregningAktivitetOverstyringEntitet entitet) {
        var periode = mapPeriode(entitet.getPeriode());
        var arbeidsgiver = mapArbeidsgiver(entitet.getArbeidsgiver().orElse(null));
        var arbeidsforholdRef = mapArbeidsforholdRef(entitet.getArbeidsforholdRef());
        var opptjeningAktivitetType = KodeverkTilKalkulusMapper.mapOpptjeningAktivitetType(entitet.getOpptjeningAktivitetType());
        var handlingType = KodeverkTilKalkulusMapper.mapBeregningAktivitetHandling(entitet.getHandling());

        var dto = new BeregningAktivitetOverstyringMigreringDto(periode, arbeidsgiver, arbeidsforholdRef, handlingType, opptjeningAktivitetType);
        settOpprettetOgEndretFelter(entitet, dto);
        return dto;
    }


    private static InternArbeidsforholdRefDto mapArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
        if (arbeidsforholdRef == null || arbeidsforholdRef.getReferanse() == null) {
            return null;
        }
        return new InternArbeidsforholdRefDto(arbeidsforholdRef.getReferanse());
    }

    private static no.nav.folketrygdloven.kalkulus.response.v1.Arbeidsgiver mapArbeidsgiver(Arbeidsgiver entitet) {
        if (entitet == null) {
            return null;
        }
        var aktørId = entitet.getAktørId() == null ? null : entitet.getAktørId().getId();
        return new no.nav.folketrygdloven.kalkulus.response.v1.Arbeidsgiver(entitet.getOrgnr(), aktørId);
    }

    private static Periode mapPeriode(AbstractLocalDateInterval entitet) {
        if (entitet == null) {
            return null;
        }
        return new Periode(entitet.getFomDato(), entitet.getTomDato());
    }

    private static void settOpprettetOgEndretFelter(BaseEntitet entitet, BaseMigreringDto dto) {
        dto.setOpprettetTidspunkt(entitet.getOpprettetTidspunkt());
        dto.setOpprettetAv(entitet.getOpprettetAv());
        dto.setEndretAv(entitet.getEndretAv());
        dto.setEndretTidspunkt(entitet.getEndretTidspunkt());
    }

}
