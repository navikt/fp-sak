package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import jakarta.xml.bind.JAXBElement;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Svangerskapspenger;

class SøknadMapperTest {

    private final YtelseSøknadMapper ytelseSøknadMapper = new YtelseSøknadMapper();

    @Test
    void mapping_av_dto_struktur_til_xml_for_arbeidstaker_i_virksomhet() {
        var behovsdato = LocalDate.of(2019, Month.SEPTEMBER, 19);
        var termindato = LocalDate.of(2020, Month.JANUARY, 1);

        var svangerskapspengerDto = new ManuellRegistreringSvangerskapspengerDto();

        svangerskapspengerDto.setSøker(ForeldreType.MOR);
        svangerskapspengerDto.setTermindato(termindato);

        var tilretteleggingArbeidsforhold = new SvpTilretteleggingVirksomhetDto();
        tilretteleggingArbeidsforhold.setOrganisasjonsnummer("123456789");
        tilretteleggingArbeidsforhold.setBehovsdato(behovsdato);
        var tilrettelegging = new SvpTilretteleggingDto();
        tilrettelegging.setDato(behovsdato);
        tilrettelegging.setTilretteleggingType(SvpTilretteleggingTypeDto.INGEN_TILRETTELEGGING);
        tilretteleggingArbeidsforhold.setTilrettelegginger(List.of(tilrettelegging));
        svangerskapspengerDto.setTilretteleggingArbeidsforhold(List.of(tilretteleggingArbeidsforhold));

        var søknad = ytelseSøknadMapper.mapSøknad(svangerskapspengerDto, lagNavBruker());

        assertThat(søknad).isNotNull();
        var object = ((JAXBElement<?>) søknad.getOmYtelse().getAny().getFirst()).getValue();
        assertThat(object).isInstanceOf(Svangerskapspenger.class);
        var svangerskapspenger = (Svangerskapspenger) object;
        assertThat(svangerskapspenger.getTermindato()).isEqualTo(termindato);
        assertThat(svangerskapspenger.getTilretteleggingListe().getTilrettelegging()).hasSize(1);
        var tilretteleggingRot = svangerskapspenger.getTilretteleggingListe().getTilrettelegging().getFirst();
        assertThat(tilretteleggingRot.getIngenTilrettelegging()).hasSize(1);
        assertThat(tilretteleggingRot.getIngenTilrettelegging().getFirst().getSlutteArbeidFom()).isEqualTo(behovsdato);
        assertThat(tilretteleggingRot.getHelTilrettelegging()).isEmpty();
        assertThat(tilretteleggingRot.getDelvisTilrettelegging()).isEmpty();
    }

    private NavBruker lagNavBruker() {
        return NavBruker.opprettNyNB(AktørId.dummy());
    }

}
