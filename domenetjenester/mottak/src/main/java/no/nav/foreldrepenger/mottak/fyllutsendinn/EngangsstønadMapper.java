package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.fyllutsendinn.frontend.PapirsoknadEngangsstonadMellomlagreDto;
import no.nav.foreldrepenger.mottak.fyllutsendinn.frontend.PapirsoknadMellomlagreDto;
import no.nav.foreldrepenger.mottak.fyllutsendinn.kilde.HvorHarDuBoddDeSiste12Manedene;
import no.nav.foreldrepenger.mottak.fyllutsendinn.kilde.HvorSkalDuBoDeNeste12Manedene;
import no.nav.foreldrepenger.mottak.fyllutsendinn.kilde.JaNei;
import no.nav.foreldrepenger.mottak.fyllutsendinn.kilde.Landvalg;
import no.nav.foreldrepenger.mottak.fyllutsendinn.kilde.Nav140507Data;

/**
 * Mapper fra Nav140507Data (fyll-ut-send-inn JSON for engangsstønad) til
 * PapirsoknadEngangsstonadMellomlagreDto for forhåndsutfylling av papirsøknad-skjema.
 */
public final class EngangsstønadMapper {

    private EngangsstønadMapper() {
    }

    public static PapirsoknadEngangsstonadMellomlagreDto tilMellomlagreDto(Nav140507Data data) {
        var dto = new PapirsoknadEngangsstonadMellomlagreDto();

        // Metadata
        dto.setFagsakYtelseType(FagsakYtelseType.ENGANGSTØNAD);
        mapFamilieHendelseType(data, dto);

        // Termin og fødsel
        mapTerminOgFødsel(data, dto);

        // Opphold i Norge
        mapOppholdINorge(data, dto);

        // Omsorg (kun ved overtakelse)
        mapOmsorg(data, dto);

        return dto;
    }

    private static void mapFamilieHendelseType(Nav140507Data data, PapirsoknadEngangsstonadMellomlagreDto dto) {
        var type = switch (data.hvaSokerDuOm()) {
            case ENGANGSSTONAD_VED_FODSEL -> FamilieHendelseType.FØDSEL;
            case ENGANGSSTONAD_VED_OVERTAKELSE_AV_FORELDREANSVARET_ELLER_OMSORGEN -> FamilieHendelseType.ADOPSJON;
        };
        dto.setFamilieHendelseType(type);
    }

    private static void mapTerminOgFødsel(Nav140507Data data, PapirsoknadEngangsstonadMellomlagreDto dto) {
        var erFødt = data.narErBarnetFodt() == Nav140507Data.NarErBarnetFodt.TILBAKE_I_TID;
        dto.setErBarnetFødt(erFødt);
        dto.setAntallBarn(data.antallBarn());

        if (erFødt) {
            firstFødselsdato(data).ifPresent(dto::setFødselsdato);
        } else {
            dto.setTermindato(data.termindatoDdMmAaaa());
            dto.setAntallBarnFraTerminbekreftelse(data.antallBarn());
        }
    }

    private static void mapOppholdINorge(Nav140507Data data, PapirsoknadEngangsstonadMellomlagreDto dto) {
        dto.setOppholdINorge(data.planleggerDuAVaereINorgePaFodselstidspunktet1() == JaNei.JA);

        var boddINorge = data.hvorHarDuBoddDeSiste12Manedene() == HvorHarDuBoddDeSiste12Manedene.KUN_BODD_I_NORGE;
        dto.setOppholdSisteTolvINorge(boddINorge);
        if (!boddINorge && data.utenlandsopphold1() != null) {
            dto.setTidligereOppholdUtenlands(data.utenlandsopphold1().stream()
                .map(r -> new PapirsoknadMellomlagreDto.UtenlandsoppholdFormValues(
                    mapLand(r.hvilketLandBoddeDuI()), r.fraDatoDdMmAaaa(), r.tilDatoDdMmAaaa()))
                .toList());
        }

        var skalBoINorge = data.hvorSkalDuBoDeNeste12Manedene() == HvorSkalDuBoDeNeste12Manedene.KUN_BO_I_NORGE;
        dto.setOppholdNesteTolvINorge(skalBoINorge);
        if (!skalBoINorge && data.utenlandsopphold() != null) {
            dto.setFremtidigeOppholdUtenlands(data.utenlandsopphold().stream()
                .map(r -> new PapirsoknadMellomlagreDto.UtenlandsoppholdFormValues(
                    mapLand(r.hvilketLandSkalDuBoI()), r.fraDatoDdMmAaaa(), r.tilDatoDdMmAaaa()))
                .toList());
        }
    }

    private static void mapOmsorg(Nav140507Data data, PapirsoknadEngangsstonadMellomlagreDto dto) {
        if (data.hvaSokerDuOm() != Nav140507Data.HvaSokerDuOm.ENGANGSSTONAD_VED_OVERTAKELSE_AV_FORELDREANSVARET_ELLER_OMSORGEN) {
            return;
        }
        var fødselsdatoer = Optional.ofNullable(data.leggTilBarnetEllerBarnasFodselsdato()).orElseGet(List::of).stream()
            .map(Nav140507Data.LeggTilBarnetEllerBarnasFodselsdatoRow::fodselsdatoDdMmAaaa)
            .toList();
        dto.setOmsorg(new PapirsoknadMellomlagreDto.OmsorgFormValues(
            data.antallBarn(),
            fødselsdatoer,
            data.datoForOmsorgsovertakelsenAvBarnetDdMmAaaa(),
            null, // ankomstdato — ikke i kildeskjema
            null  // erEktefellesBarn — ikke i kildeskjema
        ));
    }

    private static Optional<LocalDate> firstFødselsdato(Nav140507Data data) {
        return Optional.ofNullable(data.leggTilBarnetEllerBarnasFodselsdato()).orElseGet(List::of).stream()
            .map(Nav140507Data.LeggTilBarnetEllerBarnasFodselsdatoRow::fodselsdatoDdMmAaaa)
            .min(Comparator.naturalOrder());
    }

    private static String mapLand(Landvalg landvalg) {
        return new Locale.Builder().setRegion(landvalg.value()).build().getISO3Country();
    }
}
