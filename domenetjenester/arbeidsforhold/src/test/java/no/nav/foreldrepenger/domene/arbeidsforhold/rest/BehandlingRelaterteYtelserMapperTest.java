package no.nav.foreldrepenger.domene.arbeidsforhold.rest;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.RelaterteYtelserDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.TilgrensendeYtelserDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.NavBrukerBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER;
import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_SØKER;
import static org.assertj.core.api.Assertions.assertThat;

class BehandlingRelaterteYtelserMapperTest {
    private static final LocalDate I_DAG = LocalDate.now();
    private static final Saksnummer SAKSNUMMER_9999 = new Saksnummer("9999");
    private static final Saksnummer SAKSNUMMER_11111 = new Saksnummer("11111");
    private final NavBruker navBruker = new NavBrukerBuilder().medAktørId(AktørId.dummy()).build();
    private final Fagsak fagsakFødsel = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, navBruker, null, SAKSNUMMER_11111);

    @Test
    void skal_returnerer_tilgrensende_ytelser_for_soker() {
        var ytelser = List.of(
                opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                        I_DAG.plusDays(360)),
                opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(265),
                        I_DAG.plusDays(200)),
                opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5),
                        null));

        var resultatListe = BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(ytelser);

        assertThat(resultatListe).hasSize(3);
        assertThat(resultatListe.get(0).getRelatertYtelseType()).isEqualTo(RelatertYtelseType.SYKEPENGER.getKode());
        assertThat(resultatListe.get(0).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(365));
        assertThat(resultatListe.get(1).getRelatertYtelseType()).isEqualTo(RelatertYtelseType.FORELDREPENGER.getKode());
        assertThat(resultatListe.get(1).getPeriodeTilDato()).isEqualTo(I_DAG.plusDays(200));
    }

    @Test
    void skal_returnerer_tilgrensende_ytelser_for_annen_forelder() {
        var ytelser = List.of(
                opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                        I_DAG.plusDays(360)),
                opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(265),
                        I_DAG.plusDays(200)),
                opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.LØPENDE, I_DAG.minusDays(5),
                        null),
                opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.ENSLIG_FORSØRGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5),
                        null));

        var resultatListe = BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(ytelser);

        assertThat(resultatListe).hasSize(4);
        assertThat(resultatListe.get(0).getRelatertYtelseType()).isEqualTo(RelatertYtelseType.SYKEPENGER.getKode());
        assertThat(resultatListe.get(0).getStatus()).isEqualTo(RelatertYtelseTilstand.AVSLUTTET.getKode());
        assertThat(resultatListe.get(1).getRelatertYtelseType()).isEqualTo(RelatertYtelseType.FORELDREPENGER.getKode());
        assertThat(resultatListe.get(1).getStatus()).isEqualTo(RelatertYtelseTilstand.AVSLUTTET.getKode());
    }

    @Test
    void skal_mapper_fagsak_til_tilgrensendeYtelserdto() {
        fagsakFødsel.setAvsluttet();

        var tilgrensendeYtelserDto = BehandlingRelaterteYtelserMapper.mapFraFagsak(fagsakFødsel, I_DAG.minusDays(5));

        assertThat(tilgrensendeYtelserDto.getRelatertYtelseType()).isEqualTo(RelatertYtelseType.ENGANGSSTØNAD.getKode());
        assertThat(tilgrensendeYtelserDto.getStatus()).isEqualTo(FagsakStatus.AVSLUTTET.getKode());
        assertThat(tilgrensendeYtelserDto.getPeriodeTilDato()).isEqualTo(I_DAG.minusDays(5));
        assertThat(tilgrensendeYtelserDto.getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(5));
        assertThat(tilgrensendeYtelserDto.getSaksNummer()).isEqualTo(SAKSNUMMER_11111.getVerdi());
    }

    @Test
    void skal_returnerer_6_tom_tilgrensende_ytelser_for_soker() {
        @SuppressWarnings("unchecked")
        List<RelaterteYtelserDto> resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(Collections.EMPTY_LIST,
                RELATERT_YTELSE_TYPER_FOR_SØKER);

        assertThat(resultatListe).hasSize(11);
        IntStream.range(0, RELATERT_YTELSE_TYPER_FOR_SØKER.size()).forEach(i -> {
            assertThat(resultatListe.get(i).getRelatertYtelseType()).isEqualTo(RELATERT_YTELSE_TYPER_FOR_SØKER.get(i).getKode());
            assertThat(resultatListe.get(i).getTilgrensendeYtelserListe()).isEmpty();
        });
    }

    @Test
    void skal_returnerer_2_tom_tilgrensende_ytelser_for_soker() {
        @SuppressWarnings("unchecked")
        List<RelaterteYtelserDto> resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(Collections.EMPTY_LIST,
                RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER);

        assertThat(resultatListe).hasSize(2);
        IntStream.range(0, RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER.size()).forEach(i -> {
            assertThat(resultatListe.get(i).getRelatertYtelseType()).isEqualTo(RELATERT_YTELSE_TYPER_FOR_SØKER.get(i).getKode());
            assertThat(resultatListe.get(i).getTilgrensendeYtelserListe()).isEmpty();
        });
    }

    @Test
    void skal_returnerer_sortert_tilgrensende_ytelser_for_soker() {
        var tilgrensendeYtelserDtos = List.of(
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET,
                        I_DAG.minusDays(365), I_DAG.plusDays(360)),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                        I_DAG.plusDays(360)),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.LØPENDE, I_DAG.minusDays(265),
                        I_DAG.plusDays(260)),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.LØPENDE, null, null),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(165),
                        I_DAG.plusDays(160)),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.LØPENDE, I_DAG.minusDays(65),
                        I_DAG.plusDays(60)),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5), null),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.ARBEIDSAVKLARINGSPENGER, RelatertYtelseTilstand.AVSLUTTET,
                        I_DAG.minusDays(500), I_DAG.plusDays(400)));

        var resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(tilgrensendeYtelserDtos,
                RELATERT_YTELSE_TYPER_FOR_SØKER);

        assertThat(resultatListe).hasSize(11);
        assertThat(resultatListe.get(0).getRelatertYtelseType()).isEqualTo(RelatertYtelseType.FORELDREPENGER.getKode());
        assertThat(resultatListe.get(0).getTilgrensendeYtelserListe()).hasSize(1);
        assertThat(resultatListe.get(2).getRelatertYtelseType()).isEqualTo(RelatertYtelseType.SYKEPENGER.getKode());
        var sykepengerYtelserListe = resultatListe.get(2).getTilgrensendeYtelserListe();
        assertThat(sykepengerYtelserListe).hasSize(6);
        assertThat(sykepengerYtelserListe.get(0).getPeriodeFraDato()).isNull();
        assertThat(sykepengerYtelserListe.get(1).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(5));
        assertThat(sykepengerYtelserListe.get(2).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(65));
        assertThat(sykepengerYtelserListe.get(3).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(165));
        assertThat(sykepengerYtelserListe.get(4).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(265));
        assertThat(sykepengerYtelserListe.get(5).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(365));
        assertThat(resultatListe.get(4).getRelatertYtelseType()).isEqualTo(RelatertYtelseType.ARBEIDSAVKLARINGSPENGER.getKode());
        assertThat(resultatListe.get(4).getTilgrensendeYtelserListe()).hasSize(1);
    }

    @Test
    void skal_returnerer_sortert_tilgrensende_ytelser_for_annen_forelder() {
        var tilgrensendeYtelserDtos = List.of(
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                        I_DAG.plusDays(360)),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET,
                        I_DAG.minusDays(365), I_DAG.plusDays(360)),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET,
                        I_DAG.minusDays(165), I_DAG.plusDays(160)),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5),
                        null),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.plusMonths(5),
                        null),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, null, null),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG, null),
                opprettTilgrensendeYtelserDto(SAKSNUMMER_9999, RelatertYtelseType.ENGANGSSTØNAD, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(500),
                        I_DAG.plusDays(400)));

        var resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(tilgrensendeYtelserDtos,
                RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER);

        assertThat(resultatListe).hasSize(2);
        assertThat(resultatListe.get(0).getRelatertYtelseType()).isEqualTo(RelatertYtelseType.FORELDREPENGER.getKode());
        var foreldrepengerYtelserListe = resultatListe.get(0).getTilgrensendeYtelserListe();
        assertThat(foreldrepengerYtelserListe).hasSize(6);
        assertThat(foreldrepengerYtelserListe.get(0).getPeriodeFraDato()).isNull();
        assertThat(foreldrepengerYtelserListe.get(1).getPeriodeFraDato()).isEqualTo(I_DAG.plusMonths(5));
        assertThat(foreldrepengerYtelserListe.get(2).getPeriodeFraDato()).isEqualTo(I_DAG);
        assertThat(foreldrepengerYtelserListe.get(3).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(5));
        assertThat(foreldrepengerYtelserListe.get(4).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(165));
        assertThat(foreldrepengerYtelserListe.get(5).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(365));
        assertThat(resultatListe.get(1).getRelatertYtelseType()).isEqualTo(RelatertYtelseType.ENGANGSSTØNAD.getKode());
        assertThat(resultatListe.get(1).getTilgrensendeYtelserListe()).hasSize(1);
    }

    private Ytelse opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType ytelseType,
            RelatertYtelseTilstand ytelseTilstand,
            LocalDate iverksettelsesDato,
            LocalDate opphoerFomDato) {

        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var ytelserBuilder = builder.getAktørYtelseBuilder(AktørId.dummy());
        var ytelse = ytelserBuilder.getYtelselseBuilderForType(Fagsystem.FPSAK, ytelseType, new Saksnummer("22"))
                .medYtelseType(ytelseType)
                .medBehandlingsTema(TemaUnderkategori.UDEFINERT)
                .medStatus(ytelseTilstand)
                .medPeriode(opphoerFomDato == null
                        ? DatoIntervallEntitet.fraOgMed(iverksettelsesDato)
                        : DatoIntervallEntitet.fraOgMedTilOgMed(iverksettelsesDato, opphoerFomDato));

        ytelserBuilder.leggTilYtelse(ytelse);
        builder.leggTilAktørYtelse(ytelserBuilder);

        return getYtelser(builder).stream().findFirst().orElseThrow();
    }

    private Collection<Ytelse> getYtelser(InntektArbeidYtelseAggregatBuilder builder) {
        return new YtelseFilter(builder.build().getAktørYtelse().iterator().next()).getFiltrertYtelser();
    }

    private TilgrensendeYtelserDto opprettTilgrensendeYtelserDto(Saksnummer saksnummer,
            RelatertYtelseType ytelseType,
            RelatertYtelseTilstand ytelseTilstand,
            LocalDate iverksettelsesDato,
            LocalDate opphoerFomDato) {
        var tilgrensendeYtelserDto = new TilgrensendeYtelserDto();
        tilgrensendeYtelserDto.setRelatertYtelseType(ytelseType.getKode());
        tilgrensendeYtelserDto.setStatus(ytelseTilstand.getKode());
        tilgrensendeYtelserDto.setSaksNummer(saksnummer);
        tilgrensendeYtelserDto.setPeriodeFraDato(iverksettelsesDato);
        tilgrensendeYtelserDto.setPeriodeTilDato(opphoerFomDato);
        return tilgrensendeYtelserDto;
    }
}
