package no.nav.foreldrepenger.datavarehus.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.YtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.svangerskapspenger.regler.fastsettperiode.grunnlag.Inngangsvilkår;

public class StønadsstatistikkHendelse {

    private YtelseType ytelseType;
    private Bruker søker;
    private FamilieHendelse familieHendelse;
    private Saksnummer saksnummer;
    private UUID behandlingsId;
    private UUID forrigeBehandlingsId;
    private Henvisning henvisning; // behandlingsId
    private LocalDateTime vedtakstidspunkt;
    // Blir ikke avkortet - brutto inntekt per år
    private BigDecimal bruttoBeregningsgrunnlag;
    // Inngangsvilkårene fra fpsak slikt de er med type og resultat.
    private List<Inngangsvilkår> inngangsvilkårs;


    private AnnenForelder annenForelder; //aktøridId og/eller saksnummer. Ikke norske identer (fra fp søknaden). Land??
    private Dekningsgrad dekningsgrad;
    private List<TilkjentPeriode> tilkjentPerioder;
    private ForeldrepengerRettigheter foreldrepengerRettigheter; //konto saldo, utregnet ut i fra rettigheter, minsteretter




}
