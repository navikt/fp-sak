package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragsmottakerStatus;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.FinnStatusForMottakere;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.adapter.TilkjentYtelseMapper;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.BehandlingVedtakOppdrag;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.ForrigeOppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

public class FinnStatusForMottakereTest extends OppdragskontrollTjenesteTestBase {

    private ForrigeOppdragInput forrigeOppdragInputFPFP;
    private BehandlingVedtakOppdrag behandlingVedtakFP;

    @BeforeEach
    public void setup() {
        super.setUp();
        forrigeOppdragInputFPFP = mock(ForrigeOppdragInput.class);
        behandlingVedtakFP = new BehandlingVedtakOppdrag("Saksbehandler", BehandlingResultatType.FORELDREPENGER_ENDRET,
            DAGENS_DATO);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilENDRNårBrukerErMottakerBådeIForrigeBehandlingOgEndretTilkjentYtelseIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 0), List.of(true, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(1200, 0), List.of(true, false), List.of(false, false),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.ENDR);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilUENDRNårBrukerErMottakerIForrigeBehandlingOgFørEndringsdatoITilkjentYtelseIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 1000), List.of(true, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(6)), List.of(1000, 1000), List.of(true, false), List.of(false, false),
            List.of(true, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelsePeriode> tilkjentYtelsePeriodeList = new TilkjentYtelseMapper(
            FamilieYtelseType.FØDSEL).mapPerioderFomEndringsdato(revurderingBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = tilkjentYtelsePeriodeList.stream()
            .flatMap(periode -> periode.getTilkjentYtelseAndeler().stream())
            .collect(Collectors.toList());
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.UENDR);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilOPPHNårBrukerErMottakerIForrigeOgIkkeMottakerIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 1000), List.of(true, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(0, 1000), List.of(true, false), List.of(false, false),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.OPPH);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilIKKE_MOTTAKERNårBrukerErMottakerHverkenIForrigeBehandlingEllerIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(0, 1000), List.of(true, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(0, 1000), List.of(true, false), List.of(false, false),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, false);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.IKKE_MOTTAKER);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilENDRNårBrukerIkkeErMottakerIForrigeBehandlingOgMottakerIRevurderingOgDetFinnesOppdragForBrukerFraFør() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(0, 1000), List.of(true, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(1000, 1000), List.of(true, false), List.of(false, false),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.ENDR);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilNYNårBrukerIkkeErMottakerIForrigeBehandlingOgBlirOppdragsmottakerFørstegangIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(0, 1000), List.of(true, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(1000, 1000), List.of(true, false), List.of(false, false),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, false);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.NY);
    }

    @Test
    public void skalMottakerStatusForArbeidsgiverSettesTilENDRNårArbeidsgiverErMottakerBådeIForrigeBehandlingOgEndretTilkjentYtelseIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(0, 1000), List.of(true, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(0, 1200), List.of(true, false), List.of(false, false),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        List<Oppdragsmottaker> mottakerList = FinnStatusForMottakere.finnStatusForMottakerArbeidsgiver(behandlingInfoFP,
            andelerOriginal, andelerRevurdering,
            Collections.singletonList(OppdragskontrollTestVerktøy.endreTilElleveSiffer(virksomhet)));

        //Assert
        assertThat(mottakerList).hasSize(1);
        assertThat(mottakerList.get(0).getStatus()).isEqualTo(OppdragsmottakerStatus.ENDR);
    }

    @Test
    public void skalMottakerStatusForArbeidsgiverSettesTilUENDRNårArbeidsgiverErMottakerIForrigeBehandlingOgFørEndringsdatoITilkjentYtelseIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 1000), List.of(true, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(6)), List.of(1000, 1000), List.of(true, false), List.of(false, false),
            List.of(false, true));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelsePeriode> tilkjentYtelsePeriodeList = new TilkjentYtelseMapper(
            FamilieYtelseType.FØDSEL).mapPerioderFomEndringsdato(revurderingBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = tilkjentYtelsePeriodeList.stream()
            .flatMap(periode -> periode.getTilkjentYtelseAndeler().stream())
            .collect(Collectors.toList());
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        List<Oppdragsmottaker> mottakerList = FinnStatusForMottakere.finnStatusForMottakerArbeidsgiver(behandlingInfoFP,
            andelerOriginal, andelerRevurdering,
            Collections.singletonList(OppdragskontrollTestVerktøy.endreTilElleveSiffer(virksomhet)));

        //Assert
        assertThat(mottakerList).hasSize(1);
        assertThat(mottakerList.get(0).getStatus()).isEqualTo(OppdragsmottakerStatus.UENDR);
    }

    @Test
    public void skalMottakerStatusForArbeidsgiverSettesTilOPPHNårArbeidsgiverErMottakerIForrigeOgIkkeMottakerIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 1000), List.of(true, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(1000, 0), List.of(true, false), List.of(false, false),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        List<Oppdragsmottaker> mottakerList = FinnStatusForMottakere.finnStatusForMottakerArbeidsgiver(behandlingInfoFP,
            andelerOriginal, andelerRevurdering,
            Collections.singletonList(OppdragskontrollTestVerktøy.endreTilElleveSiffer(virksomhet)));

        //Assert
        assertThat(mottakerList).hasSize(1);
        assertThat(mottakerList.get(0).getStatus()).isEqualTo(OppdragsmottakerStatus.OPPH);
    }

    @Test
    public void skalMottakerStatusForArbeidsgiverSettesTilENDRNårArbeidsgiverIkkeErMottakerIForrigeBehandlingOgMottakerIRevurderingOgDetFinnesOppdragForArbeidsgiverFraFør() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 0), List.of(true, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(1000, 1000), List.of(true, false), List.of(false, false),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        List<Oppdragsmottaker> mottakerList = FinnStatusForMottakere.finnStatusForMottakerArbeidsgiver(behandlingInfoFP,
            andelerOriginal, andelerRevurdering,
            Collections.singletonList(OppdragskontrollTestVerktøy.endreTilElleveSiffer(virksomhet)));

        //Assert
        assertThat(mottakerList).hasSize(1);
        assertThat(mottakerList.get(0).getStatus()).isEqualTo(OppdragsmottakerStatus.ENDR);
    }

    @Test
    public void skalMottakerStatusForArbeidsgiverSettesTilNYNårArbeidsgiverIkkeErMottakerIForrigeBehandlingOgBlirOppdragsmottakerFørstegangIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 0), List.of(true, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(1000, 1000), List.of(true, false), List.of(false, false),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        List<Oppdragsmottaker> mottakerList = FinnStatusForMottakere.finnStatusForMottakerArbeidsgiver(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, Collections.emptyList());

        //Assert
        assertThat(mottakerList).hasSize(1);
        assertThat(mottakerList.get(0).getStatus()).isEqualTo(OppdragsmottakerStatus.NY);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilENDRNårPrivatArbeidsgiverErMottakerBådeIForrigeBehandlingOgEndretTilkjentYtelseIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(0, 1000), List.of(true, false), List.of(true, true), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(0, 1200), List.of(true, false), List.of(false, true),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.ENDR);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilENDRNårPrivatArbeidsgiverErMottakerIForrigeBehandlingOgBrukerBlirMottakerIEndretTilkjentYtelseIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(0, 1000), List.of(true, false), List.of(true, true), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(1000, 0), List.of(true, false), List.of(true, true),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.ENDR);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilENDRNårPrivatArbeidsgiverErMottakerIForrigeBehandlingOgBrukerMedArbforholdHosEnOrgBlirMottakerIEndretTilkjentYtelseIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(0, 1000), List.of(true, false), List.of(true, true), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(1000, 0), List.of(true, false), List.of(false, true),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.ENDR);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilENDRNårBrukerErMottakerIForrigeBehandlingOgPrivatArbeidsgiverBlirMottakerIEndretTilkjentYtelseIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 0), List.of(true, false), List.of(true, true), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(0, 1000), List.of(true, false), List.of(true, true),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.ENDR);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilENDRNårBrukerMedArbforholdHosEnOrgErMottakerIForrigeBehandlingOgPrivatArbeidsgiverBlirMottakerIEndretTilkjentYtelseIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 0), List.of(true, false), List.of(false, true), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(0, 1000), List.of(true, false), List.of(true, true),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.ENDR);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilUENDRNårPrivatArbeidsgiverErMottakerIForrigeBehandlingOgFørEndringsdatoITilkjentYtelseIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 1000), List.of(false, false), List.of(false, true), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(6)), List.of(1000, 1000), List.of(false, false), List.of(false, true),
            List.of(false, true));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelsePeriode> tilkjentYtelsePeriodeList = new TilkjentYtelseMapper(
            FamilieYtelseType.FØDSEL).mapPerioderFomEndringsdato(revurderingBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = tilkjentYtelsePeriodeList.stream()
            .flatMap(periode -> periode.getTilkjentYtelseAndeler().stream())
            .collect(Collectors.toList());
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.UENDR);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilOPPHNårPrivatArbeidsgiverenErMottakerIForrigeBehandlingOgBlirIkkeMottakerIEndretTilkjentYtelseIRevurderingLenger() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 1000), List.of(false, false), List.of(true, true), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(1000, 0), List.of(false, false), List.of(false, true),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.OPPH);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilOPPHNårBrukerOgPrivatArbeidsgiverErMottakereIForrigeBehandlingOgDeBlirIkkeMottakerIEndretTilkjentYtelseIRevurderingLenger() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 1000), List.of(true, false), List.of(true, true), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(1000, 1000), List.of(false, false), List.of(false, false),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.OPPH);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilIKKE_MOTTAKERNårPrivatArbeidsgiverErMottakerHverkenIForrigeBehandlingEllerIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 1000), List.of(false, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(1100, 1100), List.of(false, false), List.of(false, false),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, false);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.IKKE_MOTTAKER);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilENDRNårPrivatArbeidsgiverIkkeErMottakerIForrigeBehandlingOgMottakerIRevurderingOgDetFinnesOppdragForPrivatArbeidsgiverFraFør() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 1000), List.of(false, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(0, 1000), List.of(false, false), List.of(true, true),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, true);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.ENDR);
    }

    @Test
    public void skalMottakerStatusForBrukerSettesTilNYNårPrivatArbeidsgiverIkkeErMottakerIForrigeBehandlingOgBlirOppdragsmottakerFørstegangIRevurdering() {
        //Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet forrigeBeregningsresultatFP = lagBeregningsresultatFP(behandling, Optional.empty(),
            List.of(1000, 1000), List.of(false, false), List.of(false, false), List.of(false, false));
        BeregningsresultatEntitet revurderingBeregningsresultatFP = lagBeregningsresultatFP(revurdering,
            Optional.of(DAGENS_DATO.plusDays(1)), List.of(0, 1000), List.of(false, false), List.of(true, true),
            List.of(false, false));
        List<TilkjentYtelseAndel> andelerOriginal = getOppdragAndeler(forrigeBeregningsresultatFP);
        List<TilkjentYtelseAndel> andelerRevurdering = getOppdragAndeler(revurderingBeregningsresultatFP);
        OppdragInput behandlingInfoFP = opprettBehandlingInfoFP(revurdering, revurderingBeregningsresultatFP);

        //Act
        OppdragsmottakerStatus status = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfoFP,
            andelerOriginal, andelerRevurdering, false);

        //Assert
        assertThat(status).isEqualTo(OppdragsmottakerStatus.NY);
    }

    private List<TilkjentYtelseAndel> getOppdragAndeler(BeregningsresultatEntitet beregningsresultat) {
        TilkjentYtelse tilkjentYtelseFP = mapTilForenkletBeregningsresultatFP(beregningsresultat);
        return tilkjentYtelseFP.getTilkjentYtelsePerioder()
            .stream()
            .flatMap(oppdragPeriode -> oppdragPeriode.getTilkjentYtelseAndeler().stream())
            .filter(andel -> andel.getDagsats() > 0)
            .collect(Collectors.toList());
    }

    private TilkjentYtelse mapTilForenkletBeregningsresultatFP(BeregningsresultatEntitet beregningsresultat) {
        return new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL).map(beregningsresultat);
    }

    private OppdragInput opprettBehandlingInfoFP(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        TilkjentYtelse tilkjentYtelseFP = mapTilForenkletBeregningsresultatFP(beregningsresultat);
        return OppdragInput.builder()
            .medBehandlingId(behandling.getId())
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medFagsakYtelseType(behandling.getFagsakYtelseType())
            .medBehandlingVedtak(behandlingVedtakFP)
            .medPersonIdent(personIdent)
            .medForenkletBeregningsresultat(tilkjentYtelseFP)
            .medTilkjentYtelsePerioderFomEndringsdato(Collections.emptyList())
            .medTidligereBehandlingInfo(forrigeOppdragInputFPFP)
            .medOpphørEtterStpEllerIkkeOpphør(false)
            .medAvslåttInntrekk(false)
            .build();
    }

    private BeregningsresultatEntitet lagBeregningsresultatFP(Behandling behandling,
                                                              Optional<LocalDate> endringsdatoOpt,
                                                              List<Integer> dagsats,
                                                              List<Boolean> brukerErMottaker,
                                                              List<Boolean> medPrivatArbeidsgiver,
                                                              List<Boolean> erKunMottakerFørEndringsdato) {
        BeregningsresultatEntitet forrigeBeregningsresultatFP = buildBeregningsresultatFP(endringsdatoOpt);
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(forrigeBeregningsresultatFP, 1, 5);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(forrigeBeregningsresultatFP, 6, 10);
        for (int i = 0; i < brukerErMottaker.size(); i++) {
            String virksomhetEntitet = medPrivatArbeidsgiver.get(i) ? null : virksomhet;
            buildBeregningsresultatAndel(b1Periode_1, brukerErMottaker.get(i), dagsats.get(i), BigDecimal.valueOf(100),
                virksomhetEntitet);
            if (!erKunMottakerFørEndringsdato.get(i)) {
                buildBeregningsresultatAndel(b1Periode_2, brukerErMottaker.get(i), dagsats.get(i),
                    BigDecimal.valueOf(100), virksomhetEntitet);
            }
        }
        beregningsresultatRepository.lagre(behandling, forrigeBeregningsresultatFP);
        return forrigeBeregningsresultatFP;
    }
}
