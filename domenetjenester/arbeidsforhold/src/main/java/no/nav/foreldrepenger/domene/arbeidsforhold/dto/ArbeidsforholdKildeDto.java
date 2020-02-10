package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

class ArbeidsforholdKildeDto {
    private String navn;

    public ArbeidsforholdKildeDto(String navn) {
        this.navn = navn;
    }

    public String getNavn() {

        return navn;
    }
}
