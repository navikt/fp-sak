package no.nav.foreldrepenger.mottak.fyllutsendinn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;

import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.fyllutsendinn.frontend.PapirsoknadMellomlagreDto;
import no.nav.foreldrepenger.mottak.fyllutsendinn.kilde.FormSubmission;
import no.nav.foreldrepenger.mottak.fyllutsendinn.kilde.Nav140507Data;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class EngangsstønadMapperTest {

    @Test
    void skal_mappe_termin_til_mellomlagre_dto() throws Exception {
        var data = lesNav140507("eksempel140507-termin.json");

        var dto = EngangsstønadMapper.tilMellomlagreDto(data);

        // Metadata
        assertThat(dto.getFagsakYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(dto.getFamilieHendelseType()).isEqualTo(FamilieHendelseType.FØDSEL);

        // Termin — ikke født ennå
        assertThat(dto.getErBarnetFødt()).isFalse();
        assertThat(dto.getTermindato()).isEqualTo(LocalDate.of(2026, Month.JUNE, 30));
        assertThat(dto.getAntallBarnFraTerminbekreftelse()).isEqualTo(1);
        assertThat(dto.getAntallBarn()).isEqualTo(1);
        assertThat(dto.getFødselsdato()).isNull();

        // Opphold — bor i Norge nå, planlegger utenlandsopphold neste 12 mnd
        assertThat(dto.getOppholdINorge()).isTrue();
        assertThat(dto.getOppholdSisteTolvINorge()).isTrue();
        assertThat(dto.getOppholdNesteTolvINorge()).isFalse();
        assertThat(dto.getTidligereOppholdUtenlands()).isNull();
        assertThat(dto.getFremtidigeOppholdUtenlands()).hasSize(1);
        var fremtidig = dto.getFremtidigeOppholdUtenlands().getFirst();
        assertThat(fremtidig.land()).isEqualTo(Landkoder.SWE.getKode());
        assertThat(fremtidig.periodeFom()).isEqualTo(LocalDate.of(2026, Month.JULY, 13));
        assertThat(fremtidig.periodeTom()).isEqualTo(LocalDate.of(2026, Month.JULY, 19));

        // Ingen omsorg ved fødsel
        assertThat(dto.getOmsorg()).isNull();
    }

    @Test
    void skal_mappe_fødsel_ett_barn_til_mellomlagre_dto() throws Exception {
        var data = lesNav140507("eksempel140507-fodsel.json");

        var dto = EngangsstønadMapper.tilMellomlagreDto(data);

        // Metadata
        assertThat(dto.getFagsakYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(dto.getFamilieHendelseType()).isEqualTo(FamilieHendelseType.FØDSEL);

        // Fødsel — flere barn født, fødselsdato hentes fra fodselsdatoDdMmAaaa1
        assertThat(dto.getErBarnetFødt()).isTrue();
        assertThat(dto.getFødselsdato()).isEqualTo(LocalDate.of(2026, Month.JUNE, 1));
        assertThat(dto.getAntallBarn()).isEqualTo(1);
        assertThat(dto.getTermindato()).isEqualTo(LocalDate.of(2026, Month.JUNE, 15));

        // Opphold — alt i Norge
        assertThat(dto.getOppholdINorge()).isTrue();
        assertThat(dto.getOppholdSisteTolvINorge()).isTrue();
        assertThat(dto.getOppholdNesteTolvINorge()).isFalse();
        assertThat(dto.getTidligereOppholdUtenlands()).isNull();
        assertThat(dto.getFremtidigeOppholdUtenlands()).hasSize(1);
        assertThat(dto.getFremtidigeOppholdUtenlands().getFirst().land()).isEqualTo(Landkoder.ESP.getKode());

        // Ingen omsorg ved fødsel
        assertThat(dto.getOmsorg()).isNull();
    }


    @Test
    void skal_mappe_fødsel_flere_barn_til_mellomlagre_dto() throws Exception {
        var data = lesNav140507("eksempel140507-fodsel-flerbarn.json");

        var dto = EngangsstønadMapper.tilMellomlagreDto(data);

        // Metadata
        assertThat(dto.getFagsakYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(dto.getFamilieHendelseType()).isEqualTo(FamilieHendelseType.FØDSEL);

        // Fødsel — flere barn født, fødselsdato hentes fra fodselsdatoDdMmAaaa1
        assertThat(dto.getErBarnetFødt()).isTrue();
        assertThat(dto.getFødselsdato()).isEqualTo(LocalDate.of(2026, Month.JUNE, 1));
        assertThat(dto.getAntallBarn()).isEqualTo(2);
        assertThat(dto.getTermindato()).isEqualTo(LocalDate.of(2026, Month.JUNE, 22));

        // Opphold — alt i Norge
        assertThat(dto.getOppholdINorge()).isTrue();
        assertThat(dto.getOppholdSisteTolvINorge()).isTrue();
        assertThat(dto.getOppholdNesteTolvINorge()).isTrue();
        assertThat(dto.getTidligereOppholdUtenlands()).isNull();
        assertThat(dto.getFremtidigeOppholdUtenlands()).isNull();

        // Ingen omsorg ved fødsel
        assertThat(dto.getOmsorg()).isNull();
    }

    @Test
    void skal_mappe_termin_flere_barn_til_mellomlagre_dto() throws Exception {
        var data = lesNav140507("eksempel140507-termin-flerbarn.json");

        var dto = EngangsstønadMapper.tilMellomlagreDto(data);

        // Metadata
        assertThat(dto.getFagsakYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(dto.getFamilieHendelseType()).isEqualTo(FamilieHendelseType.FØDSEL);

        // Termin — flere barn, ikke født ennå (erBarnaFodt = nei)
        assertThat(dto.getErBarnetFødt()).isFalse();
        assertThat(dto.getTermindato()).isEqualTo(LocalDate.of(2026, Month.JUNE, 22));
        assertThat(dto.getAntallBarnFraTerminbekreftelse()).isEqualTo(2);
        assertThat(dto.getAntallBarn()).isEqualTo(2);
        assertThat(dto.getFødselsdato()).isNull();

        // Opphold — alt i Norge
        assertThat(dto.getOppholdINorge()).isTrue();
        assertThat(dto.getOppholdSisteTolvINorge()).isTrue();
        assertThat(dto.getOppholdNesteTolvINorge()).isTrue();
        assertThat(dto.getTidligereOppholdUtenlands()).isNull();
        assertThat(dto.getFremtidigeOppholdUtenlands()).isNull();

        // Ingen omsorg ved fødsel
        assertThat(dto.getOmsorg()).isNull();
    }

    @Test
    void skal_mappe_omsorgsovertakelse_til_mellomlagre_dto() throws Exception {
        var data = lesNav140507("eksempel140507-omsorg.json");

        var dto = EngangsstønadMapper.tilMellomlagreDto(data);

        // Metadata — overtakelse av omsorg mappes til adopsjon
        assertThat(dto.getFagsakYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(dto.getFamilieHendelseType()).isEqualTo(FamilieHendelseType.ADOPSJON);

        // Barnet er født
        assertThat(dto.getErBarnetFødt()).isTrue();
        assertThat(dto.getFødselsdato()).isEqualTo(LocalDate.of(2026, Month.JANUARY, 2));
        assertThat(dto.getAntallBarn()).isEqualTo(1);
        assertThat(dto.getTermindato()).isEqualTo(LocalDate.of(2026, Month.JUNE, 22));

        // Omsorg
        assertThat(dto.getOmsorg()).isNotNull();
        assertThat(dto.getOmsorg().antallBarn()).isEqualTo(1);
        assertThat(dto.getOmsorg().fødselsdato()).containsExactly(LocalDate.of(2026, Month.JANUARY, 2));
        assertThat(dto.getOmsorg().omsorgsovertakelsesdato()).isEqualTo(LocalDate.of(2026, Month.JUNE, 22));

        // Opphold — bodd i utlandet siste 12 mnd, skal bo i Norge neste 12 mnd
        assertThat(dto.getOppholdSisteTolvINorge()).isFalse();
        assertThat(dto.getTidligereOppholdUtenlands()).hasSize(1);
        assertThat(dto.getTidligereOppholdUtenlands().getFirst().land()).isEqualTo(Landkoder.DEU.getKode());
        assertThat(dto.getOppholdNesteTolvINorge()).isTrue();
        assertThat(dto.getFremtidigeOppholdUtenlands()).isNull();
    }

    @Test
    void skal_mappe_til_gyldig_json_for_frontend() throws Exception {
        var data = lesNav140507("eksempel140507-termin.json");

        var dto = EngangsstønadMapper.tilMellomlagreDto(data);
        var json = DefaultJsonMapper.toJson(dto);

        // Verifiser at JSON inneholder forventede feltnavn som frontend konsumerer
        assertThat(json).isNotBlank().contains("fagsakYtelseType", "familieHendelseType", "erBarnetFødt", "antallBarn");

        // Verifiser round-trip via base DTO
        var roundTripped = DefaultJsonMapper.fromJson(json, PapirsoknadMellomlagreDto.class);
        assertThat(roundTripped.getFagsakYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(roundTripped.getErBarnetFødt()).isFalse();
    }

    private static Nav140507Data lesNav140507(String filnavn) throws Exception {
        try (var is = EngangsstønadMapperTest.class.getResourceAsStream("/fyllutsendinn/" + filnavn)) {
            FormSubmission<Nav140507Data> submission = DefaultJsonMapper.getJsonMapper()
                .readerFor(new TypeReference<FormSubmission<Nav140507Data>>() {})
                .readValue(is);
            return submission.data().data();
        }
    }
}
