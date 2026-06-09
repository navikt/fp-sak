package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.time.LocalDate;
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
        var erFødt = erBarnFødt(data) == JaNei.JA;
        dto.setErBarnetFødt(erFødt);
        dto.setAntallBarn(data.antallBarn());
        dto.setTermindato(data.termindatoDdMmAaaa());

        if (erFødt) {
            fødselsdato(data).ifPresent(dto::setFødselsdato);
        } else {
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
        var fødselsdatoer = fødselsdato(data).map(List::of).orElseGet(List::of);
        dto.setOmsorg(new PapirsoknadMellomlagreDto.OmsorgFormValues(
            data.antallBarn(),
            fødselsdatoer,
            omsorgsovertakelsesdato(data),
            null, // ankomstdato — ikke i kildeskjema
            null  // erEktefellesBarn — ikke i kildeskjema
        ));
    }

    private static boolean flereBarn(Nav140507Data data) {
        return data.antallBarn() != null && data.antallBarn() > 1;
    }

    private static JaNei erBarnFødt(Nav140507Data data) {
        return flereBarn(data)
            ? Optional.ofNullable(data.erBarnaFodt()).orElse(data.erBarnetFodt())
            : Optional.ofNullable(data.erBarnetFodt()).orElse(data.erBarnaFodt());
    }

    private static Optional<LocalDate> fødselsdato(Nav140507Data data) {
        return flereBarn(data)
            ? Optional.ofNullable(data.fodselsdatoDdMmAaaa1()).or(() -> Optional.ofNullable(data.fodselsdatoDdMmAaaa()))
            : Optional.ofNullable(data.fodselsdatoDdMmAaaa()).or(() -> Optional.ofNullable(data.fodselsdatoDdMmAaaa1()));
    }

    private static LocalDate omsorgsovertakelsesdato(Nav140507Data data) {
        return flereBarn(data)
            ? Optional.ofNullable(data.datoForOmsorgsovertakelsenAvBarnaDdMmAaaa()).orElse(data.datoForOmsorgsovertakelsenAvBarnetDdMmAaaa())
            : Optional.ofNullable(data.datoForOmsorgsovertakelsenAvBarnetDdMmAaaa()).orElse(data.datoForOmsorgsovertakelsenAvBarnaDdMmAaaa());
    }

    private static String mapLand(Landvalg landvalg) {
        return new Locale.Builder().setRegion(landvalg.value()).build().getISO3Country();
    }
}
