package no.nav.foreldrepenger.domene.abakus.mapping;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.OffentligYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class IAYDtoMapperLagretKonverteringTest {

    private static final LocalDate DATO = LocalDate.now();
    private static final String ORGNR = KUNSTIG_ORG;
    private static final LocalDate FOM_DATO = DATO.minusDays(3);
    private static final LocalDate TOM_DATO = DATO.minusDays(2);
    private static final LocalDate ANVIST_FOM = DATO.minusDays(200);
    private static final LocalDate ANVIST_TOM = DATO.minusDays(100);
    private static final LocalDate OPPRINNELIG_IDENTDATO = DATO.minusDays(100);

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private InntektArbeidYtelseGrunnlag hentGrunnlag(Long behandlingId) {
        return iayTjeneste.hentGrunnlag(behandlingId);
    }

    @Test
    void skal_lagre_ned_inntekt_arbeid_ytelser() {
        var behandlingId = 1L;
        var aktørId = AktørId.dummy();

        var aggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandlingId);
        var aktørInntektBuilder = aggregatBuilder.getAktørInntektBuilder(aktørId);
        var inntektBuilder = aktørInntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING,
                new Opptjeningsnøkkel(null, ORGNR, null));
        var inntektspostBuilder = inntektBuilder.getInntektspostBuilder();

        var aktørArbeidBuilder = aggregatBuilder.getAktørArbeidBuilder(aktørId);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
                new Opptjeningsnøkkel(null, ORGNR, null),
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var aktørYtelseBuilder = aggregatBuilder.getAktørYtelseBuilder(aktørId);
        aktørYtelseBuilder.leggTilYtelse(lagYtelse(aktørYtelseBuilder));

        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        var permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

        var fraOgMed = DATO.minusWeeks(1);
        var tilOgMed = DATO.plusMonths(1);

        var permisjon = permisjonBuilder
                .medProsentsats(BigDecimal.valueOf(100))
                .medPeriode(fraOgMed, tilOgMed)
                .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMISJON)
                .build();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed))
                .medSisteLønnsendringsdato(fraOgMed);

        var yrkesaktivitet = yrkesaktivitetBuilder
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilPermisjon(permisjon)
                .build();

        var aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        var inntektspost = inntektspostBuilder
                .medBeløp(BigDecimal.TEN)
                .medPeriode(fraOgMed, tilOgMed)
                .medInntektspostType(InntektspostType.YTELSE)
                .medYtelse(OffentligYtelseType.UDEFINERT);

        inntektBuilder
                .leggTilInntektspost(inntektspost)
                .medArbeidsgiver(yrkesaktivitet.getArbeidsgiver())
                .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING);

        var aktørInntekt = aktørInntektBuilder
                .leggTilInntekt(inntektBuilder);

        aggregatBuilder.leggTilAktørInntekt(aktørInntekt);
        aggregatBuilder.leggTilAktørArbeid(aktørArbeid);
        aggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);

        iayTjeneste.lagreIayAggregat(behandlingId, aggregatBuilder);

        var grunnlag = hentGrunnlag(behandlingId);
        assertThat(grunnlag).isNotNull();
    }

    private YtelseBuilder lagYtelse(InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder) {
        var sakId = new Saksnummer("1200094");
        var ytelselseBuilder = aktørYtelseBuilder.getYtelselseBuilderForType(Fagsystem.FPSAK, RelatertYtelseType.SYKEPENGER, sakId);
        ytelselseBuilder.tilbakestillAnvisteYtelser();
        return ytelselseBuilder.medKilde(Fagsystem.INFOTRYGD)
                .medYtelseType(RelatertYtelseType.FORELDREPENGER)
                .medBehandlingsTema(TemaUnderkategori.UDEFINERT)
                .medStatus(RelatertYtelseTilstand.AVSLUTTET)
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FOM_DATO, TOM_DATO))
                .medSaksnummer(sakId)
                .medYtelseGrunnlag(
                        ytelselseBuilder.getGrunnlagBuilder()
                                .medOpprinneligIdentdato(OPPRINNELIG_IDENTDATO)
                                .medInntektsgrunnlagProsent(new BigDecimal(99.00))
                                .medDekningsgradProsent(new BigDecimal(98.00))
                                .medYtelseStørrelse(YtelseStørrelseBuilder.ny()
                                        .medBeløp(new BigDecimal(100000.50))
                                        .medVirksomhet(ORGNR)
                                        .build())
                                .medVedtaksDagsats(new Beløp(557))
                                .build())
                .medYtelseAnvist(ytelselseBuilder.getAnvistBuilder()
                        .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(ANVIST_FOM, ANVIST_TOM))
                        .medDagsats(new BigDecimal(500.00))
                        .medUtbetalingsgradProsent(null)
                        .build());
    }

}
