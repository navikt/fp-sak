package no.nav.foreldrepenger.mottak.hendelser.impl.oversetter;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.dødsfall.DødForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.DødHendelse;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.ForretningshendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.oversetter.DødForretningshendelseOversetter;

public class DødForretningshendelseOversetterTest {
    private static final List<String> AKTØR_ID_LISTE = singletonList(AktørId.dummy().getId());
    private static final LocalDate DØDSDATO = LocalDate.now();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DødForretningshendelseOversetter dødForretningshendelseOversetter;

    @Before
    public void before() {
        dødForretningshendelseOversetter = new DødForretningshendelseOversetter();
    }

    @Test
    public void skal_oversette_fra_json_til_DødForretningshendelse() {
        // Arrange
        DødHendelse dødHendelse = new DødHendelse(AKTØR_ID_LISTE, DØDSDATO);
        String json = JsonMapper.toJson(dødHendelse);
        ForretningshendelseDto forretningshendelse = new ForretningshendelseDto("DØD", json);

        // Act
        DødForretningshendelse resultat = dødForretningshendelseOversetter.oversett(forretningshendelse);

        // Assert
        assertThat(resultat.getForretningshendelseType()).isEqualTo(ForretningshendelseType.DØD);
        assertThat(resultat.getAktørIdListe().stream().map(AktørId::getId).collect(Collectors.toList())).isEqualTo(AKTØR_ID_LISTE);
        assertThat(resultat.getDødsdato()).isEqualTo(DØDSDATO);
    }
}
