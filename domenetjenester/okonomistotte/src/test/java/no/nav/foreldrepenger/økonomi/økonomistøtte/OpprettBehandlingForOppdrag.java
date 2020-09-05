package no.nav.foreldrepenger.økonomi.økonomistøtte;

import static java.time.Month.JANUARY;
import static no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn.KVINNE;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandling.impl.FinnAnsvarligSaksbehandler;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;


public class OpprettBehandlingForOppdrag {

    public final static long SATS = 66221L;
    public final static LocalDateTime nå = LocalDateTime.now();

    @SuppressWarnings("deprecation")
    public static Personinfo opprettPersonInfo() {
        return new Personinfo.Builder()
            .medAktørId(AktørId.dummy())
            .medPersonIdent(PersonIdent.fra("12345678901"))
            .medNavn("Kari Nordmann")
            .medFødselsdato(LocalDate.of(1990, JANUARY, 1))
            .medNavBrukerKjønn(KVINNE)
            .build();
    }

    public static ScenarioMorSøkerEngangsstønad opprettBehandlingMedTermindato() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusDays(40))
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(LocalDate.now()))
            .medAntallBarn(1);
        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusDays(40))
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(LocalDate.now().minusDays(7)))
            .medAntallBarn(1);
        return scenario;
    }

    public static void genererBehandlingOgResultat(Behandling behandling, VedtakResultatType vedtakResultatType, long antallBarn) {
        BehandlingResultatType behandlingResultatType = vedtakResultatType.equals(VedtakResultatType.INNVILGET) ?
            BehandlingResultatType.INNVILGET : BehandlingResultatType.AVSLÅTT;
        var bhRes = Behandlingsresultat.builderForInngangsvilkår()
            .medBehandlingResultatType(behandlingResultatType)
            .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING)
            .medRettenTil(RettenTil.HAR_RETT_TIL_FP)
            .medVedtaksbrev(Vedtaksbrev.INGEN)
            .buildFor(behandling);
        if (VedtakResultatType.INNVILGET.equals(vedtakResultatType)) {
            if (antallBarn <= 0) {
                throw new IllegalStateException("Ved innvilgelse må antall barn angis");
            }
            LegacyESBeregning beregning = new LegacyESBeregning(SATS, antallBarn, SATS * antallBarn, nå);
            LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, bhRes);
        }
    }

    public static void genererBehandlingOgResultatFP(Behandling behandling) {
        Behandlingsresultat.builderForInngangsvilkår()
            .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING)
            .medRettenTil(RettenTil.HAR_RETT_TIL_FP)
            .medVedtaksbrev(Vedtaksbrev.INGEN)
            .buildFor(behandling);
    }

    public static BehandlingVedtak opprettBehandlingVedtak(Behandling behandling, Behandlingsresultat behandlingsresultat, VedtakResultatType vedtakResultatType) {
        return opprettBehandlingVedtak(behandling, behandlingsresultat, vedtakResultatType, false);
    }

    public static BehandlingVedtak opprettBehandlingVedtak(Behandling behandling, Behandlingsresultat behandlingsresultat, VedtakResultatType resultatType, boolean vedtaksdatoFørIDag) {
        String ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);
        return BehandlingVedtak.builder()
            .medVedtakstidspunkt(vedtaksdatoFørIDag ? LocalDateTime.now().minusDays(3) : LocalDateTime.now())
            .medAnsvarligSaksbehandler(ansvarligSaksbehandler)
            .medVedtakResultatType(resultatType)
            .medBehandlingsresultat(behandlingsresultat)
            .build();
    }
}
