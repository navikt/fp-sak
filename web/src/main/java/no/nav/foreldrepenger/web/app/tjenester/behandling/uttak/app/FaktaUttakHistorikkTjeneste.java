package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.uttak.KodeMapper;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BekreftetOppgittPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SlettetUttakPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakDokumentasjonDto;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class FaktaUttakHistorikkTjeneste {

    private static final KodeMapper<OppholdÅrsak, UttakPeriodeType> oppholdÅrsakMapper = initOppholdÅrsakMapper();
    private static final KodeMapper<OverføringÅrsak, HistorikkAvklartSoeknadsperiodeType> overføringÅrsakMapper = initOverføringÅrsakMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String PERIODE_SEPARATOR = " , ";
    private static final String PERIODE_SEPARATOR_ENDE = " og ";

    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    FaktaUttakHistorikkTjeneste() {
        // FOR CDI proxy
    }

    @Inject
    public FaktaUttakHistorikkTjeneste(HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                       ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                       YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                       InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void byggHistorikkinnslag(List<BekreftetOppgittPeriodeDto> bekreftedePerioder,
                                     List<SlettetUttakPeriodeDto> slettedePerioder,
                                     Behandling behandling,
                                     boolean utførtAvOverstyrer) {

        HistorikkInnslagTekstBuilder tekstBuilder;
        var overstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId())
            .getArbeidsforholdOverstyringer();
        for (var bkftUttakPeriodeDto : bekreftedePerioder) {
            if (erNyPeriode(bkftUttakPeriodeDto)) {
                tekstBuilder = new HistorikkInnslagTekstBuilder();
                lagTekstBuilder(tekstBuilder, bkftUttakPeriodeDto.getBekreftetPeriode().getBegrunnelse(), utførtAvOverstyrer);
                byggHistorikkinnslagForNyperiode(behandling, bkftUttakPeriodeDto.getBekreftetPeriode(), tekstBuilder);
            } else {
                tekstBuilder = new HistorikkInnslagTekstBuilder();
                lagTekstBuilder(tekstBuilder, bkftUttakPeriodeDto.getBekreftetPeriode().getBegrunnelse(), utførtAvOverstyrer);
                byggHistorikinnslagForAvklartSøknadsperiode(behandling, bkftUttakPeriodeDto, tekstBuilder, overstyringer);
            }
        }
        if (slettedePerioder != null) {
            for (var slettet : slettedePerioder) {
                tekstBuilder = new HistorikkInnslagTekstBuilder();
                lagTekstBuilder(tekstBuilder, slettet.getBegrunnelse(), utførtAvOverstyrer);
                byggHistorikkinnslagForSlettetperiode(behandling, slettet, tekstBuilder);
            }
        }

    }

    private void lagTekstBuilder(HistorikkInnslagTekstBuilder tekstBuilder, String begrunnelse, boolean overstyring) {
        tekstBuilder
            .medBegrunnelse(begrunnelse)
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_UTTAK);
        if (overstyring) {
            tekstBuilder.medResultat(HistorikkResultatType.OVERSTYRING_FAKTA_UTTAK);
        }
    }

    private boolean erNyPeriode(BekreftetOppgittPeriodeDto bkftUttakPeriodeDto) {
        return bkftUttakPeriodeDto.getOrginalFom() == null && bkftUttakPeriodeDto.getOrginalTom() == null;
    }

    private void byggHistorikkinnslagForSlettetperiode(Behandling behandling, SlettetUttakPeriodeDto dto, HistorikkInnslagTekstBuilder tekstBuilder) {
        leggTilUttaksperiodetype(HistorikkAvklartSoeknadsperiodeType.SLETTET_SOEKNASPERIODE, tekstBuilder, dto.getUttakPeriodeType(), dto.getFom(),
            dto.getTom());
        opprettHistorikkInnslag(behandling, tekstBuilder);
    }

    private void byggHistorikkinnslagForNyperiode(Behandling behandling, KontrollerFaktaPeriodeLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder) {
        leggTilUttaksperiodetype(HistorikkAvklartSoeknadsperiodeType.NY_SOEKNADSPERIODE, tekstBuilder, dto.getUttakPeriodeType(), dto.getFom(), dto.getTom());
        opprettHistorikkInnslag(behandling, tekstBuilder);
    }

    private void byggHistorikinnslagForAvklartSøknadsperiode(Behandling behandling, BekreftetOppgittPeriodeDto dto, HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> overstyringer) {
        if (erGradering(dto)) {
            byggHistorikkinnslagForFerieEllerArbeid(behandling, dto, HistorikkAvklartSoeknadsperiodeType.GRADERING, true, tekstBuilder, overstyringer);
        } else if (erUtsettelse(dto.getBekreftetPeriode())) {
            byggHistorikkinnslagForUtsettelse(behandling, dto, tekstBuilder, overstyringer);
        } else if (erOverføring(dto.getBekreftetPeriode())) {
            byggHistorikkinnslagForOverføring(behandling, dto, tekstBuilder);
        } else if (erOpphold(dto.getBekreftetPeriode())) {
            byggHistorikkinnslagForOpphold(behandling, dto, tekstBuilder);
        } else {
            byggHistorikkinnslagForGeneraltSøknadsPeriode(behandling, dto, HistorikkAvklartSoeknadsperiodeType.UTTAK, tekstBuilder);
        }
    }

    private void byggHistorikkinnslagForOpphold(Behandling behandling, BekreftetOppgittPeriodeDto dto, HistorikkInnslagTekstBuilder tekstBuilder) {

        if (!erEndretPeriodeEllerBegrunnelseEllerResultat(dto)) {
            return;
        }

        var bekreftetPeriode = dto.getBekreftetPeriode();
        var uttakPeriodeTypeOpt = oppholdÅrsakMapper.map(bekreftetPeriode.getOppholdÅrsak());
        var uttakPeriodeType = uttakPeriodeTypeOpt.orElse(UttakPeriodeType.UDEFINERT);
        leggTilUttaksperiodetype(HistorikkAvklartSoeknadsperiodeType.OPPHOLD, tekstBuilder, uttakPeriodeType, dto.getOrginalFom(), dto.getOrginalTom());
        if (erEndretResultat(dto)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_RESULTAT_PERIODEN, null,
                finnHistorikkFeltTypeForResultat(bekreftetPeriode.erAvklartDokumentert(), bekreftetPeriode.getResultat()));
        }
        // hvis periode endret
        if (UttakPeriodeVurderingType.PERIODE_OK_ENDRET.equals(bekreftetPeriode.getResultat()) && erEndretPeriode(dto)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.AVKLART_PERIODE, null, formaterPeriode(bekreftetPeriode.getFom(), bekreftetPeriode.getTom()));
        }
        opprettHistorikkInnslag(behandling, tekstBuilder);
    }

    private void byggHistorikkinnslagForOverføring(Behandling behandling, BekreftetOppgittPeriodeDto dto, HistorikkInnslagTekstBuilder tekstBuilder) {
        var historikkOverføringType = overføringÅrsakMapper.map(dto.getBekreftetPeriode().getOverføringÅrsak())
            .orElse(HistorikkAvklartSoeknadsperiodeType.UDEFINERT);
        byggHistorikkinnslagDokumentertPeriode(behandling, dto, historikkOverføringType, tekstBuilder);
    }

    private static KodeMapper<OverføringÅrsak, HistorikkAvklartSoeknadsperiodeType> initOverføringÅrsakMapper() {
        return KodeMapper
            .medMapping(OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER, HistorikkAvklartSoeknadsperiodeType.OVERFOERING_INNLEGGELSE)
            .medMapping(OverføringÅrsak.SYKDOM_ANNEN_FORELDER, HistorikkAvklartSoeknadsperiodeType.OVERFOERING_SKYDOM)
            .medMapping(OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER, HistorikkAvklartSoeknadsperiodeType.OVERFOERING_IKKE_RETT)
            .medMapping(OverføringÅrsak.ALENEOMSORG, HistorikkAvklartSoeknadsperiodeType.OVERFOERING_ALENEOMSORG)
            .build();
    }

    private void byggHistorikkinnslagForUtsettelse(Behandling behandling, BekreftetOppgittPeriodeDto dto, HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> overstyringer) {
        var utsettelseÅrsak = dto.getBekreftetPeriode().getUtsettelseÅrsak();
        if (UtsettelseÅrsak.FERIE.equals(utsettelseÅrsak)) {
            byggHistorikkinnslagForFerieEllerArbeid(behandling, dto, HistorikkAvklartSoeknadsperiodeType.UTSETTELSE_FERIE, false, tekstBuilder, overstyringer);
        } else if (UtsettelseÅrsak.ARBEID.equals(utsettelseÅrsak)) {
            byggHistorikkinnslagForFerieEllerArbeid(behandling, dto, HistorikkAvklartSoeknadsperiodeType.UTSETTELSE_ARBEID, false, tekstBuilder, overstyringer);
        } else if (UtsettelseÅrsak.SYKDOM.equals(utsettelseÅrsak)) {
            byggHistorikkinnslagDokumentertPeriode(behandling, dto, HistorikkAvklartSoeknadsperiodeType.UTSETTELSE_SKYDOM, tekstBuilder);
        } else if (UtsettelseÅrsak.INSTITUSJON_BARN.equals(utsettelseÅrsak)) {
            byggHistorikkinnslagDokumentertPeriode(behandling, dto, HistorikkAvklartSoeknadsperiodeType.UTSETTELSE_INSTITUSJON_BARN, tekstBuilder);
        } else if (UtsettelseÅrsak.INSTITUSJON_SØKER.equals(utsettelseÅrsak)) {
            byggHistorikkinnslagDokumentertPeriode(behandling, dto, HistorikkAvklartSoeknadsperiodeType.UTSETTELSE_INSTITUSJON_SØKER, tekstBuilder);
        } else if (UtsettelseÅrsak.HV_OVELSE.equals(utsettelseÅrsak)) {
            byggHistorikkinnslagDokumentertPeriode(behandling, dto, HistorikkAvklartSoeknadsperiodeType.UTSETTELSE_HV, tekstBuilder);
        } else if (UtsettelseÅrsak.NAV_TILTAK.equals(utsettelseÅrsak)) {
            byggHistorikkinnslagDokumentertPeriode(behandling, dto, HistorikkAvklartSoeknadsperiodeType.UTSETTELSE_TILTAK_I_REGI_AV_NAV, tekstBuilder);
        }
        // UtsettelseÅrsak FRI krever ikke dokumentasjon - men bør vel lage innslag hvis SBH legger til en slik periode - hvis det skal være mulig?!
    }

    private boolean erGradering(BekreftetOppgittPeriodeDto dto) {
        return dto.getBekreftetPeriode().getArbeidstidsprosent() != null && dto.getBekreftetPeriode().getArbeidstidsprosent().compareTo(BigDecimal.ZERO) > 0;
    }

    private void byggHistorikkinnslagDokumentertPeriode(Behandling behandling,
                                                        BekreftetOppgittPeriodeDto dto,
                                                        HistorikkAvklartSoeknadsperiodeType søknadsperiodeType,
                                                        HistorikkInnslagTekstBuilder tekstBuilder) {
        if (erEndretBegrunnelse(dto) || erEndretResultat(dto)) {
            var bekreftetPeriode = dto.getBekreftetPeriode();
            var dokumentertePerioder = bekreftetPeriode.getDokumentertePerioder();

            List<LocalDateInterval> dokumenterteDatoer = dokumentertePerioder != null ? mapDokumentertPerioder(dokumentertePerioder) : Collections.emptyList();

            leggTilUttaksperiodetype(søknadsperiodeType, tekstBuilder, bekreftetPeriode.getUttakPeriodeType(), dto.getOrginalFom(), dto.getOrginalTom());

            if (bekreftetPeriode.erAvklartDokumentert() && !dokumenterteDatoer.isEmpty()) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_RESULTAT_PERIODEN, null,
                    finnHistorikkFeltTypeForPeriodeSomKreverDokumentasjon(bekreftetPeriode, true));
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.AVKLART_PERIODE, null, konvertPerioderTilString(dokumenterteDatoer));
            } else {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_RESULTAT_PERIODEN, null,
                    finnHistorikkFeltTypeForPeriodeSomKreverDokumentasjon(bekreftetPeriode, false));
            }
            opprettHistorikkInnslag(behandling, tekstBuilder);
        }
    }

    private void byggHistorikkinnslagForFerieEllerArbeid(Behandling behandling, BekreftetOppgittPeriodeDto dto,
                                                         HistorikkAvklartSoeknadsperiodeType soeknadsperiodeType, boolean gradering,
                                                         HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> overstyringer) {

        if (!erEndretPeriodeEllerArbeidsprosentEllerBegrunnelseEllerResultat(dto, gradering)) {
            return;
        }

        var orgPeriode = new LocalDateInterval(dto.getOrginalFom(), dto.getOrginalTom());
        var bekreftetPeriode = dto.getBekreftetPeriode();
        var bkftPeriode = new LocalDateInterval(bekreftetPeriode.getFom(), bekreftetPeriode.getTom());

        tekstBuilder
            .medNavnVerdiOgAvklartSøknadperiode(soeknadsperiodeType, arbeidsgiverNavnOgIdentifikator(dto, overstyringer).orElse(""),
                uttakperiodeTypeOgTidsperiodeText(bekreftetPeriode.getUttakPeriodeType(), orgPeriode.getFomDato(), orgPeriode.getTomDato()));
        if (erEndretResultat(dto)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_RESULTAT_PERIODEN, null,
                finnHistorikkFeltTypeForResultat(bekreftetPeriode.erAvklartDokumentert(), bekreftetPeriode.getResultat()));
        }
        // hvis periode endret
        if (UttakPeriodeVurderingType.PERIODE_OK_ENDRET.equals(bekreftetPeriode.getResultat()) && erEndretPeriode(dto)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.AVKLART_PERIODE, null, formaterPeriode(bkftPeriode.getFomDato(), bkftPeriode.getTomDato()));
        }
        // hvis arbeidsprosent endret
        if (gradering && erEndretArbeidstidsprosent(dto)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.ANDEL_ARBEID, formaterArbeidstidsProsent(dto.getOriginalArbeidstidsprosent()),
                formaterArbeidstidsProsent(dto.getBekreftetPeriode().getArbeidstidsprosent()));
        }
        opprettHistorikkInnslag(behandling, tekstBuilder);
    }

    private Optional<String> arbeidsgiverNavnOgIdentifikator(BekreftetOppgittPeriodeDto dto, List<ArbeidsforholdOverstyring> overstyringer) {
        var arbeidsgiver = dto.getBekreftetPeriode().getArbeidsgiver();
        if (arbeidsgiver != null) {
            if (arbeidsgiver.erVirksomhet()) {
                return Optional.of(virksomhetArbeidsgiver(arbeidsgiver, overstyringer));
            }
            return Optional.of(personArbeidsgiver(arbeidsgiver, overstyringer));
        }
        return Optional.empty();
    }

    private String personArbeidsgiver(ArbeidsgiverLagreDto arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        return arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver.person(arbeidsgiver.getAktørId()), overstyringer);
    }

    private String virksomhetArbeidsgiver(ArbeidsgiverLagreDto arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        var arbeidsgiverIdentifikator = arbeidsgiver.getIdentifikator();
        return arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator), overstyringer);
    }

    private String uttakperiodeTypeOgTidsperiodeText(UttakPeriodeType uttakPeriodeType, LocalDate fom, LocalDate tom) {
        String text;
        var periodeText = formaterPeriode(fom, tom);
        if (Objects.equals(uttakPeriodeType, UttakPeriodeType.UDEFINERT)) {
            text = periodeText;
        } else {
            text = formaterStringMedPeriodeType(uttakPeriodeType.getNavn(), periodeText);
        }
        return text;
    }

    private void byggHistorikkinnslagForGeneraltSøknadsPeriode(Behandling behandling, BekreftetOppgittPeriodeDto dto,
                                                               HistorikkAvklartSoeknadsperiodeType soeknadsperiodeType,
                                                               HistorikkInnslagTekstBuilder tekstBuilder) {
        if (!erEndretPeriodeEllerBegrunnelseEllerResultat(dto)) {
            return;
        }
        var bekreftetPeriode = dto.getBekreftetPeriode();
        var bkftPeriode = new LocalDateInterval(bekreftetPeriode.getFom(), bekreftetPeriode.getTom());

        leggTilUttaksperiodetype(soeknadsperiodeType, tekstBuilder, dto.getBekreftetPeriode().getUttakPeriodeType(), dto.getOrginalFom(), dto.getOrginalTom());
        if (erEndretResultat(dto)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_RESULTAT_PERIODEN, null,
                finnHistorikkFeltTypeForResultat(bekreftetPeriode.erAvklartDokumentert(), bekreftetPeriode.getResultat()));
        }
        // hvis periode endret
        if (UttakPeriodeVurderingType.PERIODE_OK_ENDRET.equals(bekreftetPeriode.getResultat()) && erEndretPeriode(dto)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.AVKLART_PERIODE, null, formaterPeriode(bkftPeriode.getFomDato(), bkftPeriode.getTomDato()));
        }
        opprettHistorikkInnslag(behandling, tekstBuilder);
    }

    private void leggTilUttaksperiodetype(HistorikkAvklartSoeknadsperiodeType soeknadsperiodeType,
                                          HistorikkInnslagTekstBuilder tekstBuilder,
                                          UttakPeriodeType uttakPeriodeType,
                                          LocalDate fom,
                                          LocalDate tom) {
        tekstBuilder.medAvklartSøknadperiode(soeknadsperiodeType, uttakperiodeTypeOgTidsperiodeText(uttakPeriodeType, fom, tom));
    }

    private String formaterStringMedPeriodeType(String s, String formattedPeriode) {
        return s + " " + formattedPeriode;
    }

    private String formaterArbeidstidsProsent(BigDecimal prosent) {
        return prosent != null ? prosent + "%" : null;
    }

    private void opprettHistorikkInnslag(Behandling behandling, HistorikkInnslagTekstBuilder tekstBuilder) {
        var innslag = new Historikkinnslag();
        innslag.setType(HistorikkinnslagType.UTTAK);
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setBehandlingId(behandling.getId());
        tekstBuilder.build(innslag);
        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }

    private boolean erEndretPeriodeEllerBegrunnelseEllerResultat(BekreftetOppgittPeriodeDto dto) {
        var erEndret = erEndretPeriode(dto);
        erEndret = erEndretBegrunnelse(dto) || erEndretResultat(dto) || erEndret;
        return erEndret;
    }

    private boolean erEndretPeriodeEllerArbeidsprosentEllerBegrunnelseEllerResultat(BekreftetOppgittPeriodeDto dto, boolean gradering) {

        var erEndret = erEndretPeriode(dto);
        if (gradering) {
            erEndret = erEndretArbeidstidsprosent(dto) || erEndret;
        }
        erEndret = erEndretBegrunnelse(dto) || erEndretResultat(dto) || erEndret;
        return erEndret;
    }

    private boolean erEndretBegrunnelse(BekreftetOppgittPeriodeDto dto) {
        return !Objects.equals(dto.getOriginalBegrunnelse(), dto.getBekreftetPeriode().getBegrunnelse());
    }

    private boolean erEndretPeriode(BekreftetOppgittPeriodeDto dto) {
        return !dto.getOrginalFom().equals(dto.getBekreftetPeriode().getFom()) ||
            !dto.getOrginalTom().equals(dto.getBekreftetPeriode().getTom());
    }

    private boolean erEndretResultat(BekreftetOppgittPeriodeDto dto) {
        return !dto.getOriginalResultat().equals(dto.getBekreftetPeriode().getResultat());
    }

    private boolean erEndretArbeidstidsprosent(BekreftetOppgittPeriodeDto dto) {
        return dto.getOriginalArbeidstidsprosent().compareTo(dto.getBekreftetPeriode().getArbeidstidsprosent()) != 0;
    }

    private List<LocalDateInterval> mapDokumentertPerioder(List<UttakDokumentasjonDto> dokumentertePerioder) {
        return dokumentertePerioder.stream()
            .map(this::mapDokumentertPeriode)
            .collect(Collectors.toList());
    }

    private LocalDateInterval mapDokumentertPeriode(UttakDokumentasjonDto kontrollerFaktaPeriodeDto) {
        Objects.requireNonNull(kontrollerFaktaPeriodeDto, "kontrollerFaktaPeriodeDto"); // NOSONAR $NON-NLS-1$
        return new LocalDateInterval(kontrollerFaktaPeriodeDto.getFom(), kontrollerFaktaPeriodeDto.getTom());
    }

    private HistorikkEndretFeltVerdiType finnHistorikkFeltTypeForResultat(boolean avklartPeriode, UttakPeriodeVurderingType vurderingType) {
        if (avklartPeriode) {
            return finnBasertPåVurderingType(vurderingType);
        }
        return HistorikkEndretFeltVerdiType.FASTSETT_RESULTAT_PERIODEN_AVKLARES_IKKE;
    }

    private HistorikkEndretFeltVerdiType finnBasertPåVurderingType(UttakPeriodeVurderingType vurderingType) {
        return UttakPeriodeVurderingType.PERIODE_OK.equals(vurderingType) ? HistorikkEndretFeltVerdiType.FASTSETT_RESULTAT_GRADERING_AVKLARES
            : HistorikkEndretFeltVerdiType.FASTSETT_RESULTAT_ENDRE_SOEKNADSPERIODEN;
    }

    private HistorikkEndretFeltVerdiType finnHistorikkFeltTypeForPeriodeSomKreverDokumentasjon(KontrollerFaktaPeriodeLagreDto bekreftetPeriode, boolean dokumentert) {
        if (erUtsettelseMedÅrsak(bekreftetPeriode, UtsettelseÅrsak.SYKDOM) || erOverføringSkydom(bekreftetPeriode)) {
            return dokumentert ? HistorikkEndretFeltVerdiType.FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT
                : HistorikkEndretFeltVerdiType.FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT_IKKE;
        }
        if (erUtsettelseInnleggelse(bekreftetPeriode) || erOverføringInnleggelse(bekreftetPeriode)) {
            return dokumentert ? HistorikkEndretFeltVerdiType.FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT
                : HistorikkEndretFeltVerdiType.FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT_IKKE;
        }
        if (erUtsettelseMedÅrsak(bekreftetPeriode, UtsettelseÅrsak.HV_OVELSE)) {
            return dokumentert ? HistorikkEndretFeltVerdiType.FASTSETT_RESULTAT_PERIODEN_HV_DOKUMENTERT
                : HistorikkEndretFeltVerdiType.FASTSETT_RESULTAT_PERIODEN_HV_DOKUMENTERT_IKKE;
        }
        if (erUtsettelseMedÅrsak(bekreftetPeriode, UtsettelseÅrsak.NAV_TILTAK)) {
            return dokumentert ? HistorikkEndretFeltVerdiType.FASTSETT_RESULTAT_PERIODEN_NAV_TILTAK_DOKUMENTERT
                : HistorikkEndretFeltVerdiType.FASTSETT_RESULTAT_PERIODEN_NAV_TILTAK_DOKUMENTERT_IKKE;
        }
        return null;
    }

    private boolean erOverføringSkydom(KontrollerFaktaPeriodeLagreDto bekreftetPeriode) {
        return erOverføring(bekreftetPeriode) && OverføringÅrsak.SYKDOM_ANNEN_FORELDER.equals(bekreftetPeriode.getOverføringÅrsak());
    }

    private boolean erOverføring(KontrollerFaktaPeriodeLagreDto bekreftetPeriode) {
        return bekreftetPeriode.getOverføringÅrsak() != null && !Årsak.UKJENT.getKode().equals(bekreftetPeriode.getOverføringÅrsak().getKode());
    }

    private boolean erOverføringInnleggelse(KontrollerFaktaPeriodeLagreDto bekreftetPeriode) {
        return bekreftetPeriode.getOverføringÅrsak() != null && !Årsak.UKJENT.getKode().equals(bekreftetPeriode.getOverføringÅrsak().getKode())
            && OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER.equals(bekreftetPeriode.getOverføringÅrsak());
    }

    private boolean erUtsettelseMedÅrsak(KontrollerFaktaPeriodeLagreDto bekreftetPeriode, UtsettelseÅrsak årsak) {
        return erUtsettelse(bekreftetPeriode) && årsak.equals(bekreftetPeriode.getUtsettelseÅrsak());
    }

    private boolean erUtsettelse(KontrollerFaktaPeriodeLagreDto bekreftetPeriode) {
        return bekreftetPeriode.getUtsettelseÅrsak() != null && !Årsak.UKJENT.getKode().equals(bekreftetPeriode.getUtsettelseÅrsak().getKode());
    }

    private boolean erUtsettelseInnleggelse(KontrollerFaktaPeriodeLagreDto bekreftetPeriode) {
        return bekreftetPeriode.getUtsettelseÅrsak() != null &&
            !Årsak.UKJENT.getKode().equals(bekreftetPeriode.getUtsettelseÅrsak().getKode()) &&
            (UtsettelseÅrsak.INSTITUSJON_SØKER.equals(bekreftetPeriode.getUtsettelseÅrsak())
                || UtsettelseÅrsak.INSTITUSJON_BARN.equals(bekreftetPeriode.getUtsettelseÅrsak()));
    }

    private String formaterPeriode(LocalDate fom, LocalDate tom) {
        return formatDate(fom) + " - " + formatDate(tom);
    }

    private boolean erOpphold(KontrollerFaktaPeriodeLagreDto bekreftetPeriode) {
        return bekreftetPeriode.getOppholdÅrsak() != null && !Årsak.UKJENT.getKode().equals(bekreftetPeriode.getOppholdÅrsak().getKode());
    }

    private String formatDate(LocalDate localDate) {
        return DATE_FORMATTER.format(localDate);
    }

    private String konvertPerioderTilString(List<LocalDateInterval> perioder) {
        var result = new StringBuilder();
        var perioderList = perioder.stream().map(periode -> formaterPeriode(periode.getFomDato(), periode.getTomDato())).collect(Collectors.toList());
        var lastIndex = perioderList.size() - 1;
        result.append(lastIndex == 0 ? perioderList.get(lastIndex)
            : perioderList.subList(0, lastIndex).stream().collect(Collectors.joining(PERIODE_SEPARATOR)).concat(PERIODE_SEPARATOR_ENDE)
            .concat(perioderList.get(lastIndex)));
        return result.toString();
    }

    private static KodeMapper<OppholdÅrsak, UttakPeriodeType> initOppholdÅrsakMapper() {
        return KodeMapper
            .medMapping(OppholdÅrsak.KVOTE_FELLESPERIODE_ANNEN_FORELDER, UttakPeriodeType.FELLESPERIODE)
            .medMapping(OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER, UttakPeriodeType.MØDREKVOTE)
            .medMapping(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER, UttakPeriodeType.FEDREKVOTE)
            .medMapping(OppholdÅrsak.KVOTE_FORELDREPENGER_ANNEN_FORELDER, UttakPeriodeType.FORELDREPENGER)
            .build();
    }

    /**
     * Historikkinnslag for avklar annen forelder har ikke rett
     */
    public void byggHistorikkinnslagForAvklarAnnenforelderHarIkkeRett(AvklarAnnenforelderHarRettDto annenforelderHarIkkeRettDto, AksjonspunktOppdaterParameter param,
                                                                      boolean endretVurderingAvMorsUføretrygd, Boolean tidligereVurderingAvUføretrygd) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());
        var rettAvklaring = ytelseFordelingAggregat.getAnnenForelderRettAvklaring();
        Boolean harAnnenForeldreRettBekreftetVersjon = null;

        if (rettAvklaring.isPresent()) {
            harAnnenForeldreRettBekreftetVersjon = rettAvklaring.get();
        }
        historikkApplikasjonTjeneste.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER,
            konvertBooleanTilVerdiForAnnenforelderHarRett(harAnnenForeldreRettBekreftetVersjon),
            konvertBooleanTilVerdiForAnnenforelderHarRett(annenforelderHarIkkeRettDto.getAnnenforelderHarRett()));
        if (endretVurderingAvMorsUføretrygd) {
            historikkApplikasjonTjeneste.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.MOR_MOTTAR_UFØRETRYGD,
                tidligereVurderingAvUføretrygd, annenforelderHarIkkeRettDto.getAnnenforelderMottarUføretrygd());
        }

        historikkApplikasjonTjeneste.tekstBuilder()
            .medBegrunnelse(annenforelderHarIkkeRettDto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OMSORG_OG_RETT);
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilVerdiForAnnenforelderHarRett(Boolean annenforelderHarRett) {
        if (annenforelderHarRett == null) {
            return null;
        }
        return annenforelderHarRett ? HistorikkEndretFeltVerdiType.ANNEN_FORELDER_HAR_RETT : HistorikkEndretFeltVerdiType.ANNEN_FORELDER_HAR_IKKE_RETT;
    }
}
