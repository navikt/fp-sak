package no.nav.foreldrepenger.domene.vedtak.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.InfotrygdHendelse;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.InfotrygdHendelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.Meldingstype;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class IdentifiserOverlappendeInfotrygdYtelseTjenesteTest {

    private static final String INNVILGET = Meldingstype.INFOTRYGD_INNVILGET.getType();
    private static final String ANNULERT = Meldingstype.INFOTRYGD_ANNULLERT.getType();
    private static final String OPPHOERT = Meldingstype.INFOTRYGD_OPPHOERT.getType();
    private static final String ENDRET = Meldingstype.INFOTRYGD_ENDRET.getType();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private BeregningsresultatRepository beregningsresultatRepository;

    @Mock
    private BehandlingOverlappInfotrygdRepository overlappRepository;

    private IdentifiserOverlappendeInfotrygdYtelseTjeneste tjeneste;

    @Mock
    private InfotrygdHendelseTjeneste infotrygdHendelseTjeneste;

    @Mock
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Mock
    private InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag;

    private Behandling behandling;

    private LocalDate startdatoVLYtelse;

    private BehandlingVedtak behandlingVedtak;

    @Before
    public void oppsett() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagMocked();
        this.startdatoVLYtelse = LocalDate.now();
        behandlingVedtak = lagVedtak(VedtakResultatType.INNVILGET);
    }

    // CASE 1:
    // Løpende ytelse: Ja, infotrygd ytelse opphører samme dag som FP
    // Nyeste hendelse: Innvilget
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, lik
    // Skal iverksettes: Nei
    // VedtakResultatType: Avslag
    @Test
    public void skal_iverksettes_når_vedtak_avslått() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse, startdatoVLYtelse.plusDays(1), startdatoVLYtelse.plusDays(2), startdatoVLYtelse.plusDays(3))
        );
        AktørYtelse infotrygdAktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse, RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, infotrygdAktørYtelse);
        behandlingVedtak = lagVedtak(VedtakResultatType.AVSLAG);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isEmpty();
    }

    // CASE 1:
    // Løpende ytelse: Ja, infotrygd ytelse opphører samme dag som FP
    // Nyeste hendelse: Innvilget
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, lik
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_iverksette_når_infotrygd_ytelse_opphører_samme_dag_som_FP_starter_og_nyeste_hendelse_er_innvilget_med_fom_lik_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse, startdatoVLYtelse.plusDays(1), startdatoVLYtelse.plusDays(2), startdatoVLYtelse.plusDays(3))
        );
        AktørYtelse infotrygdAktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse, RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, infotrygdAktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();
    }

    // CASE 2:
    // Løpende ytelse: Ja, infotrygd ytelse opphører samme dag som FP
    // Nyeste hendelse: Innvilget
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, tidligere
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_infotrygd_ytelse_opphører_samme_dag_som_FP_starter_og_nyeste_hendelse_er_innvilget_med_fom_før_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse.minusDays(2), startdatoVLYtelse.minusDays(1), startdatoVLYtelse, startdatoVLYtelse.plusDays(2))
        );
        AktørYtelse infotrygdAktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse, RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, infotrygdAktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 3:
    // Løpende ytelse: Ja, infotrygd ytelse opphører samme dag som FP
    // Nyeste hendelse: Innvilget
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Nei, etter
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_infotrygd_ytelse_opphører_samme_dag_som_FP_starter_og_nyeste_hendelse_er_innvilget_med_fom_etter_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse.plusDays(1), startdatoVLYtelse.plusDays(2), startdatoVLYtelse.plusDays(3), startdatoVLYtelse.plusDays(4))
        );
        AktørYtelse infotrygdAktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse, RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, infotrygdAktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 4:
    // Løpende ytelse: Ja, FP opphører samme dag som infotrygd ytelse
    // Nyeste hendelse: Innvilget
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, lik
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_FP_opphører_samme_dag_som_infotrygd_ytelse_starter_og_nyeste_hendelse_er_innvilget_med_fom_lik_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse, startdatoVLYtelse.plusDays(1), startdatoVLYtelse.plusDays(2), startdatoVLYtelse.plusDays(3))
        );
        AktørYtelse infotrygdAktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(7), startdatoVLYtelse.plusDays(14), RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(2)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(3), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, infotrygdAktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 5:
    // Løpende ytelse: Ja, FP opphører samme dag som infotrygd ytelse
    // Nyeste hendelse: Innvilget
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, tidligere
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_FP_opphører_samme_dag_som_infotrygd_ytelse_starter_og_nyeste_hendelse_er_innvilget_med_fom_før_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse.minusDays(1), startdatoVLYtelse, startdatoVLYtelse.plusDays(1), startdatoVLYtelse.plusDays(2))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(7), startdatoVLYtelse.plusDays(14), RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(2)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(3), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 6:
    // Løpende ytelse: Ja, FP opphører samme dag som infotrygd ytelse
    // Nyeste hendelse: Innvilget
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Nei, etter
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_FP_opphører_samme_dag_som_infotrygd_ytelse_starter_og_nyeste_hendelse_er_innvilget_med_fom_etter_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse.plusDays(1), startdatoVLYtelse.plusDays(2), startdatoVLYtelse.plusDays(3), startdatoVLYtelse.plusDays(4))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(7), startdatoVLYtelse.plusDays(14), RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(2)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(3), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 7:
    // Løpende ytelse: Ja, infotrygd ytelse opphører samme dag som FP
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, lik
    // Skal iverksettes: Ja
    @Test
    public void skal_kunne_iverksette_når_infotrygd_ytelse_opphører_samme_dag_som_FP_starter_og_nyeste_hendelse_er_opphoert_med_fom_lik_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(2), startdatoVLYtelse.minusDays(1), startdatoVLYtelse)
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse, RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isEmpty();

    }

    // CASE 8:
    // Løpende ytelse: Ja, infotrygd ytelse opphører samme dag som FP
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, tidligere
    // Skal iverksettes: Ja
    @Test
    public void skal_kunne_iverksette_når_infotrygd_ytelse_opphører_samme_dag_som_FP_starter_og_nyeste_hendelse_er_opphoert_med_fom_før_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(3), startdatoVLYtelse.minusDays(2), startdatoVLYtelse.minusDays(1))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse, RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isEmpty();

    }

    // CASE 9:
    // Løpende ytelse: Ja, FP opphører samme dag som infotrygd ytelse
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, lik
    // Skal iverksettes: Ja
    @Test
    public void skal_kunne_iverksette_når_FP_opphører_samme_dag_som_infotrygd_ytelse_starter_og_nyeste_hendelse_er_opphoert_med_fom_lik_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(2), startdatoVLYtelse.minusDays(1), startdatoVLYtelse)
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(7), startdatoVLYtelse.plusDays(14), RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(2)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(3), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isEmpty();

    }

    // CASE 10:
    // Løpende ytelse: Ja, FP opphører samme dag som infotrygd ytelse
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, tidligere
    // Skal iverksettes: Ja
    @Test
    public void skal_kunne_iverksette_når_FP_opphører_samme_dag_som_infotrygd_ytelse_starter_og_nyeste_hendelse_er_opphoert_med_fom_før_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(3), startdatoVLYtelse.minusDays(2), startdatoVLYtelse.minusDays(1))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(7), startdatoVLYtelse.plusDays(14), RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(2)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(3), startdatoVLYtelse.plusDays(7))
        );
        // Infotrygd: startdatoVLYtelse.plusDays(7) -> startdatoVLYtelse.plusDays(14)
        // VL: startdatoVLYtelse -> startdatoVLYtelse.plusDays(7)
        // Hendelse: OPPHØR

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isEmpty();
    }


    // CASE 11:
    // Løpende ytelse: Ja, infotrygd ytelse opphører samme dag som FP
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Nei, etter
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_infotrygd_ytelse_opphører_samme_dag_som_FP_starter_og_nyeste_hendelse_er_opphoert_med_fom_etter_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(1), startdatoVLYtelse, startdatoVLYtelse.plusDays(1))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse, RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 12:
    // Løpende ytelse: Ja
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Nei
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_FP_opphører_samme_dag_som_infotrygd_ytelse_starter_og_nyeste_hendelse_er_opphoert_med_fom_etter_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(1), startdatoVLYtelse, startdatoVLYtelse.plusDays(1))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(7), startdatoVLYtelse.plusDays(14), RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(2)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(3), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 13:
    // Løpende ytelse: Ja, infotrygd ytelse opphører samme dag som FP
    // Nyeste hendelse: Ingen
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: N/A
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_infotrygd_ytelse_opphører_samme_dag_som_FP_starter_og_ingen_nyeste_hendelse() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            Collections.emptyList(),
            Collections.emptyList()
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse, RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 14:
    // Løpende ytelse: Ja, FP opphører samme dag som infotrygd ytelse
    // Nyeste hendelse: Ingen
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: N/A
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_FP_opphører_samme_dag_som_infotrygd_ytelse_starter_og_ingen_nyeste_hendelse() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            Collections.emptyList(),
            Collections.emptyList()
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(7), startdatoVLYtelse.plusDays(14), RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(3)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(4), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 15:
    // Løpende ytelse: Nei, infotrygd ytlese opphører dagen før FP
    // Nyeste hendelse: Innvilet
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, tidligere
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_infotrygd_ytelse_opphører_dagen_før_FP_starter_og_nyeste_hendelse_er_innvilget_med_fom_før_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse.minusDays(2), startdatoVLYtelse.minusDays(1), startdatoVLYtelse, startdatoVLYtelse.plusDays(2))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse.minusDays(1), RelatertYtelseTilstand.AVSLUTTET);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 16:
    // Løpende ytelse: Nei, infotrygd ytelse opphører dagen for FP
    // Nyeste hendelse: Innvilet
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Nei, etter
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_infotrygd_ytelse_opphører_dagen_før_FP_starter_og_nyeste_hendelse_er_innvilget_med_fom_etter_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse.plusDays(1), startdatoVLYtelse.plusDays(2), startdatoVLYtelse.plusDays(3), startdatoVLYtelse.plusDays(4))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse.minusDays(1), RelatertYtelseTilstand.AVSLUTTET);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 17:
    // Løpende ytelse: Nei, infotrygd ytelse opphører dagen før FP
    // Nyeste hendelse: Innvilet
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, lik
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_infotrygd_ytelse_opphører_dagen_før_FP_starter_og_nyeste_hendelse_er_innvilget_med_fom_lik_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse, startdatoVLYtelse.plusDays(1), startdatoVLYtelse.plusDays(2), startdatoVLYtelse.plusDays(3))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse.minusDays(1), RelatertYtelseTilstand.AVSLUTTET);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 18:
    // Løpende ytelse: Nei, FP opphører dagen før infotrygd ytelse
    // Nyeste hendelse: Innvilet
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, tidligere
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_FP_opphører_dagen_før_infotrygd_ytelse_starter_og_nyeste_hendelse_er_innvilget_med_fom_før_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse.minusDays(2), startdatoVLYtelse.minusDays(1), startdatoVLYtelse, startdatoVLYtelse.plusDays(2))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(8), startdatoVLYtelse.plusDays(12), RelatertYtelseTilstand.AVSLUTTET);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(3)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(4), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 19:
    // Løpende ytelse: Nei, FP opphører dagen før infotrygd ytelse
    // Nyeste hendelse: Innvilet
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Nei, etter
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_FP_opphører_dagen_før_infotrygd_ytelse_starter_og_nyeste_hendelse_er_innvilget_med_fom_etter_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse.plusDays(1), startdatoVLYtelse.plusDays(2), startdatoVLYtelse.plusDays(3), startdatoVLYtelse.plusDays(4))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(8), startdatoVLYtelse.plusDays(12), RelatertYtelseTilstand.AVSLUTTET);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(3)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(4), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 20:
    // Løpende ytelse: Nei, FP opphører dagen før infotrygd ytelse
    // Nyeste hendelse: Innvilet
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, lik
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_FP_opphører_dagen_før_infotrygd_ytelse_starter_og_nyeste_hendelse_er_innvilget_med_fom_lik_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT, ANNULERT),
            List.of(startdatoVLYtelse, startdatoVLYtelse.plusDays(1), startdatoVLYtelse.plusDays(2), startdatoVLYtelse.plusDays(3))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(8), startdatoVLYtelse.plusDays(12), RelatertYtelseTilstand.AVSLUTTET);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(3)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(4), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 21:
    // Løpende ytelse: Nei, infotrygd ytlese opphører dagen før FP
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, tidligere
    // Skal iverksettes: Ja
    @Test
    public void skal_kunne_iverksette_når_infotrygd_ytelse_opphører_dagen_før_FP_starter_og_nyeste_hendelse_er_opphoert_med_fom_før_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(3), startdatoVLYtelse.minusDays(2), startdatoVLYtelse.minusDays(1))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse.minusDays(1), RelatertYtelseTilstand.AVSLUTTET);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isEmpty();

    }

    // CASE 22:
    // Løpende ytelse: Nei, infotrygd ytlese opphører dagen før FP
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, lik
    // Skal iverksettes: Ja
    @Test
    public void skal_kunne_iverksette_når_infotrygd_ytelse_opphører_dagen_før_FP_starter_og_nyeste_hendelse_er_opphoert_med_fom_lik_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(2), startdatoVLYtelse.minusDays(1), startdatoVLYtelse)
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse.minusDays(1), RelatertYtelseTilstand.AVSLUTTET);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isEmpty();

    }

    // CASE 23:
    // Løpende ytelse: Nei, FP opphører dagen før infortrygd ytelse
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, tidligere
    // Skal iverksettes: Ja
    @Test
    public void skal_kunne_iverksette_når_FP_opphører_dagen_før_infotrygd_ytelse_starter_og_nyeste_hendelse_er_opphoert_med_fom_før_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(3), startdatoVLYtelse.minusDays(2), startdatoVLYtelse.minusDays(1))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(8), startdatoVLYtelse.plusDays(12), RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(3)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(4), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isEmpty();

    }

    // CASE 24:
    // Løpende ytelse: Nei, FP opphører dagen før infortrygd ytelse
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Ja, lik
    // Skal iverksettes: Ja
    @Test
    public void skal_kunne_iverksette_når_FP_opphører_dagen_før_infotrygd_ytelse_starter_og_nyeste_hendelse_er_opphoert_med_fom_lik_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(2), startdatoVLYtelse.minusDays(1), startdatoVLYtelse)
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(8), startdatoVLYtelse.plusDays(12), RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(3)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(4), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isEmpty();

    }

    // CASE 25:
    // Løpende ytelse: Nei, infotrygd ytelse opphører før FP starter
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Nei, etter
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_infotrygd_ytelse_opphører_dagen_før_FP_starter_og_nyeste_hendelse_er_opphoert_med_fom_etter_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(1), startdatoVLYtelse, startdatoVLYtelse.plusDays(1))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse.minusDays(1), RelatertYtelseTilstand.AVSLUTTET);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }


    // CASE 26:
    // Løpende ytelse: Nei, FP opphører før infortrygd ytelse starter
    // Nyeste hendelse: Opphoert
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: Nei, etter
    // Skal iverksettes: Nei
    @Test
    public void skal_ikke_kunne_iverksette_når_FP_opphører_dagen_før_infotrygd_ytelse_starter_og_nyeste_hendelse_er_opphoert_med_fom_etter_startdato() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            List.of(INNVILGET, ENDRET, OPPHOERT),
            List.of(startdatoVLYtelse.minusDays(1), startdatoVLYtelse, startdatoVLYtelse.plusDays(1))
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(8), startdatoVLYtelse.plusDays(12), RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(3)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(4), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isPresent();

    }

    // CASE 27:
    // Løpende ytelse: Nei, infortrygd ytlese opphører før FP starter
    // Nyeste hendelse: Ingen
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: N/A
    // Skal iverksettes: Ja
    @Test
    public void skal_kunne_iverksette_når_infotrygd_ytelse_opphører_dagen_før_FP_starter_og_ingen_nyeste_hendelse() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            Collections.emptyList(),
            Collections.emptyList()
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.minusDays(5), startdatoVLYtelse.minusDays(1), RelatertYtelseTilstand.AVSLUTTET);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusWeeks(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusWeeks(1).plusDays(1), startdatoVLYtelse.plusWeeks(2))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isEmpty();

    }

    // CASE 28:
    // Løpende ytelse: Nei, FP opphører før infotrygd ytelse starter
    // Nyeste hendelse: Ingen
    // Hendelse FOM dato tidligere/lik startdato for VL ytelse: N/A
    // Skal iverksettes: Ja
    @Test
    public void skal_kunne_iverksette_når_FP_opphører_dagen_før_infotrygd_ytelse_starter_og_ingen_nyeste_hendelse() {

        // Arrange
        List<InfotrygdHendelse> hendelser = lagInfotrygdHendelse(
            Collections.emptyList(),
            Collections.emptyList()
        );
        AktørYtelse aktørYtelse = lagAktørYtelse(startdatoVLYtelse.plusDays(8), startdatoVLYtelse.plusDays(12), RelatertYtelseTilstand.LØPENDE);
        opprettOgLagreBeregningsResultat(
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse, startdatoVLYtelse.plusDays(3)),
            DatoIntervallEntitet.fraOgMedTilOgMed(startdatoVLYtelse.plusDays(4), startdatoVLYtelse.plusDays(7))
        );

        opprettOgMockFellesTjenester(hendelser, aktørYtelse);

        // Act
        Optional<BehandlingOverlappInfotrygd> overlapp = tjeneste.vurder(behandling, behandlingVedtak);

        // Assert
        assertThat(overlapp).isEmpty();

    }

    private BehandlingVedtak lagVedtak(VedtakResultatType vedtakResultatType) {
        Behandlingsresultat behandlingsresultat = behandling.getBehandlingsresultat();
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
            .medAnsvarligSaksbehandler("E2354345")
            .medVedtakResultatType(vedtakResultatType)
            .medIverksettingStatus(IverksettingStatus.IKKE_IVERKSATT)
            .medBehandlingsresultat(behandlingsresultat)
            .build();
        return behandlingVedtak;
    }

    private List<InfotrygdHendelse> lagInfotrygdHendelse(List<String> typeList, List<LocalDate> datoList) {
        List<InfotrygdHendelse> hendelser = new ArrayList<>();
        int ix = 0;
        long sekvensnummer = 0L;
        for (String type : typeList) {
            InfotrygdHendelse hendelse = InfotrygdHendelse.builder()
                .medAktørId(1001L)
                .medIdentDato("20180101")
                .medFom(datoList.get(ix++))
                .medSekvensnummer(sekvensnummer++)
                .medType(type)
                .medTypeYtelse(RelatertYtelseType.SYKEPENGER.getKode())
                .build();
            hendelser.add(hendelse);
        }
        return hendelser;
    }

    private AktørYtelse lagAktørYtelse(LocalDate fom, LocalDate tom, RelatertYtelseTilstand relatertYtelseTilstand) {
        YtelseBuilder ytelseBuilder = lagYtelse(fom, tom, relatertYtelseTilstand);
        return InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty())
            .medAktørId(behandling.getAktørId())
            .leggTilYtelse(ytelseBuilder)
            .build();
    }

    private YtelseBuilder lagYtelse(LocalDate fom, LocalDate tom, RelatertYtelseTilstand relatertYtelseTilstand) {
        return YtelseBuilder.oppdatere(Optional.empty())
            .medYtelseType(RelatertYtelseType.FORELDREPENGER)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medKilde(Fagsystem.INFOTRYGD)
            .medStatus(relatertYtelseTilstand);
    }

    private void opprettOgLagreBeregningsResultat(DatoIntervallEntitet... perioder) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        for (DatoIntervallEntitet periode : perioder) {
            BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(periode.getFomDato(), periode.getTomDato())
                .build(beregningsresultat);
            lagBeregningsresultatAndel(beregningsresultatPeriode);
            beregningsresultat.addBeregningsresultatPeriode(beregningsresultatPeriode);
        }

        Mockito.when(beregningsresultatRepository.hentUtbetBeregningsresultat(Mockito.any())).thenReturn(Optional.of(beregningsresultat));
    }

    private void lagBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode) {
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsatsFraBg(100)
            .medDagsats(100)
            .build(beregningsresultatPeriode);
    }

    private void opprettOgMockFellesTjenester(List<InfotrygdHendelse> hendelser, AktørYtelse aktørYtelse) {
        tjeneste = new IdentifiserOverlappendeInfotrygdYtelseTjeneste(beregningsresultatRepository, infotrygdHendelseTjeneste, inntektArbeidYtelseTjeneste, overlappRepository);
        when(infotrygdHendelseTjeneste.hentHendelsesListFraInfotrygdFeed(behandling)).thenReturn(hendelser);

        InntektArbeidYtelseGrunnlag iayg = Mockito.mock(InntektArbeidYtelseGrunnlag.class);
        when(iayg.getAktørYtelseFraRegister(Mockito.any())).thenReturn(Optional.of(aktørYtelse));

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(Mockito.any())).thenReturn(Optional.of(iayg));
    }

}
