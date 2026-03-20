package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.time.LocalDate;

/** Prefilled address validity period (addressValidity component). */
public class AddressValidity {

    private LocalDate fraOgMedDato;
    private LocalDate tilOgMedDato;

    public AddressValidity() {}

    public LocalDate getFraOgMedDato() { return fraOgMedDato; }
    public void setFraOgMedDato(LocalDate fraOgMedDato) { this.fraOgMedDato = fraOgMedDato; }

    public LocalDate getTilOgMedDato() { return tilOgMedDato; }
    public void setTilOgMedDato(LocalDate tilOgMedDato) { this.tilOgMedDato = tilOgMedDato; }
}
