package no.nav.foreldrepenger.domene.medlem.impl.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapKildeType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;

public class HentMedlemskapFraRestTest {

    private static final long MEDL_ID_1 = 2663947L;
    private static final long MEDL_ID_2 = 2663948L;
    private static final long MEDL_ID_3 = 666L;

    @Test
    public void roundtrip_rest_1()  {
        var mrest = new Medlemskapsunntak(MEDL_ID_1,
            LocalDate.of(2019, 8, 1),
            LocalDate.of(2019, 12, 31),
            "Full",
            "MEDFT",
            "ENDL",
            "UZB",
            null,
            true,
            new Medlemskapsunntak.Sporingsinformasjon(LocalDate.of(2020, 5, 26), "AVGSYS"),
            new Medlemskapsunntak.Studieinformasjon("VUT"));
        var json = JsonObjectMapper.toJson(mrest, null);
        var dser = JsonObjectMapper.fromJson(json, Medlemskapsunntak.class);
        assertThat(mrest).isEqualTo(dser);
    }

    @Test
    public void roundtrip_rest_2()  {
        var mrest = new Medlemskapsunntak(MEDL_ID_2,
            LocalDate.of(2019, 8, 1),
            LocalDate.of(2019, 12, 31),
            MedlemskapDekningType.FTL_2_9_1_a.getKode(),
            "MEDFT",
            MedlemskapType.ENDELIG.getKode(),
            null,
            null,
            true,
            new Medlemskapsunntak.Sporingsinformasjon(LocalDate.of(2020, 5, 26), MedlemskapKildeType.AVGSYS.getKode()),
            null);
        var json = JsonObjectMapper.toJson(mrest, null);
        var dser = JsonObjectMapper.fromJson(json, Medlemskapsunntak.class);
        assertThat(mrest).isEqualTo(dser);
    }

    @Test
    public void roundtrip_rest_3()  {
        var mrest = new Medlemskapsunntak(MEDL_ID_3,
            LocalDate.of(2019, 1, 1),
            LocalDate.of(2019, 2, 28),
            MedlemskapDekningType.FULL.getKode(),
            "MEDFT",
            MedlemskapType.UNDER_AVKLARING.getKode(),
            null,
            null,
            true,
            new Medlemskapsunntak.Sporingsinformasjon(LocalDate.of(2019, 5, 26), MedlemskapKildeType.LAANEKASSEN.getKode()),
            null);
        var json = JsonObjectMapper.toJson(mrest, null);
        var dser = JsonObjectMapper.fromJson(json, Medlemskapsunntak.class);
        assertThat(mrest).isEqualTo(dser);
    }
}
