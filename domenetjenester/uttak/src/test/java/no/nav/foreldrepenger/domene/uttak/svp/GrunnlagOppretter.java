package no.nav.foreldrepenger.domene.uttak.svp;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.*;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

class GrunnlagOppretter {

    private final UttakRepositoryProvider repositoryProvider;

    GrunnlagOppretter(UttakRepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;

    }

    Behandling lagreBehandling() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        return scenario.lagre(repositoryProvider);
    }

    SvpGrunnlagEntitet lagTilrettelegging(Behandling behandling) {
        var behovFraDato = LocalDate.of(2019, Month.APRIL, 1);
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(behovFraDato)
            .medIngenTilrettelegging(behovFraDato, behovFraDato, SvpTilretteleggingFomKilde.SØKNAD)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medKopiertFraTidligereBehandling(false)
            .medMottattTidspunkt(LocalDateTime.now())
            .build();
        return new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
    }

    SvpGrunnlagEntitet lagTilretteleggingMedOpphold(Long behandlingId) {
        var behovFraDato = LocalDate.now();
        var behovFraDato2 = LocalDate.now().plusMonths(1);

        var tilr2Fom1 = new TilretteleggingFOM.Builder()
            .medFomDato(behovFraDato)
            .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
            .medTidligstMottattDato(behovFraDato.minusDays(5))
            .build();

        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(behovFraDato)
            .medTilretteleggingFraDatoer(List.of(tilr2Fom1))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123456789"))
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false)
            .medAvklarteOpphold(List.of(opprettOpphold(behovFraDato.plusDays(2), behovFraDato.plusDays(4), SvpOppholdÅrsak.SYKEPENGER),
                opprettOpphold(behovFraDato2, behovFraDato2.plusWeeks(4), SvpOppholdÅrsak.FERIE)))
            .build();

        return new SvpGrunnlagEntitet.Builder().medBehandlingId(behandlingId)
            .medOverstyrteTilrettelegginger(List.of(tilrettelegging))
            .build();
    }


    private SvpAvklartOpphold opprettOpphold(LocalDate fom, LocalDate tom, SvpOppholdÅrsak årsak) {
        return SvpAvklartOpphold.Builder.nytt().medOppholdPeriode(fom, tom).medOppholdÅrsak( årsak).build();
    }

    void lagreUttaksgrenser(Long behandlingId, LocalDate mottaksdato) {
        var uttaksperiodegrense = new Uttaksperiodegrense(mottaksdato);
        repositoryProvider.getUttaksperiodegrenseRepository().lagre(behandlingId, uttaksperiodegrense);
    }
}
