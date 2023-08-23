package no.nav.foreldrepenger.domene.arbeidsforhold;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class YtelseTestHelper {

    public static InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder leggTilYtelseMedAnvist(
            InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder,
            LocalDate fom, LocalDate tom,
            RelatertYtelseTilstand relatertYtelseTilstand, String saksnummer, RelatertYtelseType ytelseType) {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        var ytelseBuilder = aktørYtelseBuilder.getYtelselseBuilderForType(Fagsystem.INFOTRYGD, ytelseType,
                new Saksnummer(saksnummer));
        ytelseBuilder.medPeriode(periode);
        ytelseBuilder.medStatus(relatertYtelseTilstand);
        ytelseBuilder.medYtelseAnvist(
                ytelseBuilder.getAnvistBuilder().medAnvistPeriode(periode).medUtbetalingsgradProsent(Stillingsprosent.HUNDRED.getVerdi()).build());
        aktørYtelseBuilder.leggTilYtelse(ytelseBuilder);
        return aktørYtelseBuilder;
    }

    public static InntektArbeidYtelseAggregatBuilder opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AktørId aktørId, InternArbeidsforholdRef ref,
            DatoIntervallEntitet periode, ArbeidType type,
            BigDecimal prosentsats, Arbeidsgiver arbeidsgiver,
            VersjonType versjonType) {
        var builder = InntektArbeidYtelseAggregatBuilder
                .oppdatere(Optional.empty(), versjonType);

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);

        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
                new Opptjeningsnøkkel(ref, arbeidsgiver.getIdentifikator(), null), type);

        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periode, false);
        var permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder.medProsentsats(prosentsats)
            .medSisteLønnsendringsdato(periode.getFomDato())
            .medBeskrivelse("Ser greit ut");
        var ansettelsesPeriode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periode, true);

        var permisjon = permisjonBuilder
                .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.UTDANNINGSPERMISJON)
                .medPeriode(periode.getFomDato(), periode.getTomDato())
                .medProsentsats(BigDecimal.valueOf(100))
                .build();

        yrkesaktivitetBuilder
                .medArbeidType(type)
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(ref)
                .leggTilPermisjon(permisjon)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilAktivitetsAvtale(ansettelsesPeriode);

        var aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        builder.leggTilAktørArbeid(aktørArbeid);

        return builder;
    }

}
