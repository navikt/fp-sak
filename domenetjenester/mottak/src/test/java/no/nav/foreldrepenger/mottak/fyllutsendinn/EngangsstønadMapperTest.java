package no.nav.foreldrepenger.mottak.fyllutsendinn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

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
        assertThat(dto.getTermindato()).isEqualTo(LocalDate.of(2026, 5, 17));
        assertThat(dto.getAntallBarnFraTerminbekreftelse()).isEqualTo(1);
        assertThat(dto.getAntallBarn()).isEqualTo(1);
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
    void skal_mappe_fødsel_med_utenlandsopphold_til_mellomlagre_dto() throws Exception {
        var data = lesNav140507("eksempel140507-fodsel.json");

        var dto = EngangsstønadMapper.tilMellomlagreDto(data);

        // Metadata
        assertThat(dto.getFagsakYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(dto.getFamilieHendelseType()).isEqualTo(FamilieHendelseType.FØDSEL);

        // Fødsel — barnet er født
        assertThat(dto.getErBarnetFødt()).isTrue();
        assertThat(dto.getFødselsdato()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(dto.getAntallBarn()).isEqualTo(2);
        assertThat(dto.getTermindato()).isNull();

        // Opphold — bodd i utlandet siste 12 mnd
        assertThat(dto.getOppholdINorge()).isTrue();
        assertThat(dto.getOppholdSisteTolvINorge()).isFalse();
        assertThat(dto.getOppholdNesteTolvINorge()).isTrue();

        // Tidligere utenlandsopphold
        assertThat(dto.getTidligereOppholdUtenlands()).hasSize(1);
        var opphold = dto.getTidligereOppholdUtenlands().getFirst();
        assertThat(opphold.land()).isEqualTo("SWE");
        assertThat(opphold.periodeFom()).isEqualTo(LocalDate.of(2025, 12, 1));
        assertThat(opphold.periodeTom()).isEqualTo(LocalDate.of(2025, 12, 31));

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
