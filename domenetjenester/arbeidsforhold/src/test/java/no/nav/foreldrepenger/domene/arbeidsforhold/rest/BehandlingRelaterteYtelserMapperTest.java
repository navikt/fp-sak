package no.nav.foreldrepenger.domene.arbeidsforhold.rest;

import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER;
import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_SØKER;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.RelaterteYtelserDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.TilgrensendeYtelser;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.NavBrukerBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

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
            opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5), null));

        var resultatListe = BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(ytelser);

        assertThat(resultatListe).hasSize(3);
        assertThat(resultatListe.get(0).relatertYtelseType()).isEqualTo(RelatertYtelseType.SYKEPENGER);
        assertThat(resultatListe.get(0).periodeFra()).isEqualTo(I_DAG.minusDays(365));
        assertThat(resultatListe.get(1).relatertYtelseType()).isEqualTo(RelatertYtelseType.FORELDREPENGER);
        assertThat(resultatListe.get(1).periodeTil()).isEqualTo(I_DAG.plusDays(200));
    }

    @Test
    void skal_returnerer_tilgrensende_ytelser_for_annen_forelder() {
        var ytelser = List.of(
            opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                I_DAG.plusDays(360)),
            opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(265),
                I_DAG.plusDays(200)),
            opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.LØPENDE, I_DAG.minusDays(5), null),
            opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType.ENSLIG_FORSØRGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5), null));

        var resultatListe = BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(ytelser);

        assertThat(resultatListe).hasSize(4);
        assertThat(resultatListe.get(0).relatertYtelseType()).isEqualTo(RelatertYtelseType.SYKEPENGER);
        assertThat(resultatListe.get(0).statusNavn()).isEqualTo(RelatertYtelseTilstand.AVSLUTTET.getNavn());
        assertThat(resultatListe.get(1).relatertYtelseType()).isEqualTo(RelatertYtelseType.FORELDREPENGER);
        assertThat(resultatListe.get(1).statusNavn()).isEqualTo(RelatertYtelseTilstand.AVSLUTTET.getNavn());
    }

    @Test
    void skal_mapper_fagsak_til_tilgrensendeYtelserdto() {
        fagsakFødsel.setAvsluttet();

        var tilgrensendeYtelser = BehandlingRelaterteYtelserMapper.mapFraFagsak(fagsakFødsel, I_DAG.minusDays(5));

        assertThat(tilgrensendeYtelser.relatertYtelseType()).isEqualTo(RelatertYtelseType.ENGANGSTØNAD);
        assertThat(tilgrensendeYtelser.statusNavn()).isEqualTo(FagsakStatus.AVSLUTTET.getNavn());
        assertThat(tilgrensendeYtelser.periodeTil()).isEqualTo(I_DAG.minusDays(5));
        assertThat(tilgrensendeYtelser.periodeFra()).isEqualTo(I_DAG.minusDays(5));
        assertThat(tilgrensendeYtelser.saksNummer()).isEqualTo(SAKSNUMMER_11111);
    }

    @Test
    void skal_returnerer_6_tom_tilgrensende_ytelser_for_soker() {
        @SuppressWarnings("unchecked") List<RelaterteYtelserDto> resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(
            Collections.EMPTY_LIST, RELATERT_YTELSE_TYPER_FOR_SØKER);

        assertThat(resultatListe).hasSize(11);
        IntStream.range(0, RELATERT_YTELSE_TYPER_FOR_SØKER.size()).forEach(i -> {
            assertThat(resultatListe.get(i).relatertYtelseNavn()).isEqualTo(RELATERT_YTELSE_TYPER_FOR_SØKER.get(i).getNavn());
            assertThat(resultatListe.get(i).tilgrensendeYtelserListe()).isEmpty();
        });
    }

    @Test
    void skal_returnerer_2_tom_tilgrensende_ytelser_for_soker() {
        @SuppressWarnings("unchecked") List<RelaterteYtelserDto> resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(
            Collections.EMPTY_LIST, RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER);

        assertThat(resultatListe).hasSize(2);
        IntStream.range(0, RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER.size()).forEach(i -> {
            assertThat(resultatListe.get(i).relatertYtelseNavn()).isEqualTo(RELATERT_YTELSE_TYPER_FOR_SØKER.get(i).getNavn());
            assertThat(resultatListe.get(i).tilgrensendeYtelserListe()).isEmpty();
        });
    }

    @Test
    void skal_returnerer_sortert_tilgrensende_ytelser_for_soker() {
        var tilgrensendeYtelser = List.of(
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                I_DAG.plusDays(360)),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                I_DAG.plusDays(360)),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.LØPENDE, I_DAG.minusDays(265),
                I_DAG.plusDays(260)),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.LØPENDE, null, null),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(165),
                I_DAG.plusDays(160)),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.LØPENDE, I_DAG.minusDays(65),
                I_DAG.plusDays(60)),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5), null),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.ARBEIDSAVKLARINGSPENGER, RelatertYtelseTilstand.AVSLUTTET,
                I_DAG.minusDays(500), I_DAG.plusDays(400)));

        var resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(tilgrensendeYtelser, RELATERT_YTELSE_TYPER_FOR_SØKER);

        assertThat(resultatListe).hasSize(11);
        assertThat(resultatListe.get(0).relatertYtelseNavn()).isEqualTo(RelatertYtelseType.FORELDREPENGER.getNavn());
        assertThat(resultatListe.get(0).tilgrensendeYtelserListe()).hasSize(1);
        assertThat(resultatListe.get(2).relatertYtelseNavn()).isEqualTo(RelatertYtelseType.SYKEPENGER.getNavn());
        var sykepengerYtelserListe = resultatListe.get(2).tilgrensendeYtelserListe();
        assertThat(sykepengerYtelserListe).hasSize(6);
        assertThat(sykepengerYtelserListe.get(0).periodeFraDato()).isNull();
        assertThat(sykepengerYtelserListe.get(1).periodeFraDato()).isEqualTo(I_DAG.minusDays(5));
        assertThat(sykepengerYtelserListe.get(2).periodeFraDato()).isEqualTo(I_DAG.minusDays(65));
        assertThat(sykepengerYtelserListe.get(3).periodeFraDato()).isEqualTo(I_DAG.minusDays(165));
        assertThat(sykepengerYtelserListe.get(4).periodeFraDato()).isEqualTo(I_DAG.minusDays(265));
        assertThat(sykepengerYtelserListe.get(5).periodeFraDato()).isEqualTo(I_DAG.minusDays(365));
        assertThat(resultatListe.get(4).relatertYtelseNavn()).isEqualTo(RelatertYtelseType.ARBEIDSAVKLARINGSPENGER.getNavn());
        assertThat(resultatListe.get(4).tilgrensendeYtelserListe()).hasSize(1);
        assertThat(resultatListe.get(4).tilgrensendeYtelserListe().get(0).saksNummer()).isEqualTo(SAKSNUMMER_9999.getVerdi());
    }

    @Test
    void skal_returnerer_sortert_tilgrensende_ytelser_for_annen_forelder() {
        var tilgrensendeYtelser = List.of(
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                I_DAG.plusDays(360)),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                I_DAG.plusDays(360)),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(165),
                I_DAG.plusDays(160)),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5), null),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.plusMonths(5), null),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, null, null),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG, null),
            opprettTilgrensendeYtelser(SAKSNUMMER_9999, RelatertYtelseType.ENGANGSTØNAD, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(500),
                I_DAG.plusDays(400)));

        var resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(tilgrensendeYtelser,
            RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER);

        assertThat(resultatListe).hasSize(2);
        assertThat(resultatListe.get(0).relatertYtelseNavn()).isEqualTo(RelatertYtelseType.FORELDREPENGER.getNavn());
        var foreldrepengerYtelserListe = resultatListe.get(0).tilgrensendeYtelserListe();
        assertThat(foreldrepengerYtelserListe).hasSize(6);
        assertThat(foreldrepengerYtelserListe.get(0).periodeFraDato()).isNull();
        assertThat(foreldrepengerYtelserListe.get(1).periodeFraDato()).isEqualTo(I_DAG.plusMonths(5));
        assertThat(foreldrepengerYtelserListe.get(2).periodeFraDato()).isEqualTo(I_DAG);
        assertThat(foreldrepengerYtelserListe.get(3).periodeFraDato()).isEqualTo(I_DAG.minusDays(5));
        assertThat(foreldrepengerYtelserListe.get(4).periodeFraDato()).isEqualTo(I_DAG.minusDays(165));
        assertThat(foreldrepengerYtelserListe.get(5).periodeFraDato()).isEqualTo(I_DAG.minusDays(365));
        assertThat(resultatListe.get(1).relatertYtelseNavn()).isEqualTo(RelatertYtelseType.ENGANGSTØNAD.getNavn());
        assertThat(resultatListe.get(1).tilgrensendeYtelserListe()).hasSize(1);
        assertThat(resultatListe.get(1).tilgrensendeYtelserListe().get(0).saksNummer()).isEqualTo(SAKSNUMMER_9999.getVerdi());
    }

    private Ytelse opprettBuilderForBehandlingRelaterteYtelser(RelatertYtelseType ytelseType,
                                                               RelatertYtelseTilstand ytelseTilstand,
                                                               LocalDate iverksettelsesDato,
                                                               LocalDate opphoerFomDato) {

        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var ytelserBuilder = builder.getAktørYtelseBuilder(AktørId.dummy());
        var ytelse = ytelserBuilder.getYtelselseBuilderForType(Fagsystem.FPSAK, ytelseType, new Saksnummer("22"))
            .medYtelseType(ytelseType)
            .medStatus(ytelseTilstand)
            .medPeriode(
                opphoerFomDato == null ? DatoIntervallEntitet.fraOgMed(iverksettelsesDato) : DatoIntervallEntitet.fraOgMedTilOgMed(iverksettelsesDato,
                    opphoerFomDato));

        ytelserBuilder.leggTilYtelse(ytelse);
        builder.leggTilAktørYtelse(ytelserBuilder);

        return getYtelser(builder).stream().findFirst().orElseThrow();
    }

    private Collection<Ytelse> getYtelser(InntektArbeidYtelseAggregatBuilder builder) {
        return new YtelseFilter(builder.build().getAktørYtelse().iterator().next()).getFiltrertYtelser();
    }

    private TilgrensendeYtelser opprettTilgrensendeYtelser(Saksnummer saksnummer,
                                                           RelatertYtelseType ytelseType,
                                                           RelatertYtelseTilstand ytelseTilstand,
                                                           LocalDate iverksettelsesDato,
                                                           LocalDate opphoerFomDato) {
        return new TilgrensendeYtelser(ytelseType, iverksettelsesDato, opphoerFomDato, ytelseTilstand.getNavn(), saksnummer);
    }
}
