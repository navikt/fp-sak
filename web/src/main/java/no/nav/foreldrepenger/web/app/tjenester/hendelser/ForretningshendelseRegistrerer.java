package no.nav.foreldrepenger.web.app.tjenester.hendelser;

import no.nav.foreldrepenger.kontrakter.abonnent.HendelseDto;

public interface ForretningshendelseRegistrerer<T extends HendelseDto> {
    EnkelRespons registrer(T hendelseDto);
}
