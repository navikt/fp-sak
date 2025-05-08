package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk;

import com.fasterxml.jackson.annotation.JsonValue;

public enum VilkårTypeDto {

    FØDSELSVILKÅRET_MOR("FP_VK_1"),
    FØDSELSVILKÅRET_FAR_MEDMOR("FP_VK_11"),
    ADOPSJONSVILKARET_FORELDREPENGER("FP_VK_16"),
    MEDLEMSKAPSVILKÅRET("FP_VK_2"),
    MEDLEMSKAPSVILKÅRET_FORUTGÅENDE("FP_VK_2_F"),
    MEDLEMSKAPSVILKÅRET_LØPENDE("FP_VK_2_L"),
    SØKNADSFRISTVILKÅRET("FP_VK_3"),
    ADOPSJONSVILKÅRET_ENGANGSSTØNAD("FP_VK_4"),
    OMSORGSVILKÅRET("FP_VK_5"),
    FORELDREANSVARSVILKÅRET_2_LEDD("FP_VK_8"),
    FORELDREANSVARSVILKÅRET_4_LEDD("FP_VK_33"),
    SØKERSOPPLYSNINGSPLIKT("FP_VK_34"),
    OPPTJENINGSPERIODEVILKÅR("FP_VK_21"),
    OPPTJENINGSVILKÅRET("FP_VK_23"),
    BEREGNINGSGRUNNLAGVILKÅR("FP_VK_41"),
    SVANGERSKAPSPENGERVILKÅR("SVP_VK_1"),
    ;

    @JsonValue
    private String kode;

    VilkårTypeDto(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
