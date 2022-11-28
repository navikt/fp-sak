package no.nav.foreldrepenger.behandling.steg.uttak.svp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;

class SvpHelper {

    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    private SvangerskapspengerRepository svangerskapspengerRepository;

    public SvpHelper(BehandlingRepositoryProvider behandlingRepositoryProvider, SvangerskapspengerRepository svpRepository) {
        this.behandlingRepositoryProvider = behandlingRepositoryProvider;
        this.svangerskapspengerRepository = svpRepository;
    }

    Behandling lagreBehandling() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        return scenario.lagre(behandlingRepositoryProvider);
    }

    Behandling lagreRevurdering(Behandling behandling) {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        return scenario.lagre(behandlingRepositoryProvider);
    }

    void lagreTerminbekreftelse(Behandling behandling, LocalDate termindato, LocalDate... fødselsdatoer) {
        var familieHendelseBuilder = byggAggregat(termindato, fødselsdatoer);
        behandlingRepositoryProvider.getFamilieHendelseRepository().lagre(behandling, familieHendelseBuilder);
    }

    void lagreIngenTilrettelegging(Behandling behandling, LocalDate jordmorsdato, LocalDate tidligstMottattDato) {
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medIngenTilrettelegging(jordmorsdato, tidligstMottattDato)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
                .medMottattTidspunkt(LocalDateTime.now())
                .medKopiertFraTidligereBehandling(false)
                .build();
        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
                .medBehandlingId(behandling.getId())
                .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
                .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }

    void lagreDelvisTilrettelegging(Behandling behandling, LocalDate jordmorsdato, LocalDate tilretteleggingFom, BigDecimal arbeidsprosent) {
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medDelvisTilrettelegging(tilretteleggingFom, arbeidsprosent, jordmorsdato)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false)
            .build();
        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }

    private FamilieHendelseBuilder byggAggregat(LocalDate termindato, LocalDate... fødselsdatoer) {
        var builder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET);
        var terminBuilder = builder.getTerminbekreftelseBuilder();
        terminBuilder.medTermindato(termindato);
        terminBuilder.medUtstedtDato(termindato.minusMonths(7));
        Arrays.stream(fødselsdatoer).forEach(builder::leggTilBarn);
        return builder.medTerminbekreftelse(terminBuilder);
    }

}
