/**
 * Hendelser som oppstår under prosessering av behandling eller ved andre kall til modulen Behandlingskontroll -
 * som utgjør et nåløye for alle operasjoner som endrer tilstand på behandling eller aksjonspunkt
 * <h2>Hendelser</h2>
 * <ul>
 *     <li> {@link no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStegStatusEvent} - når BehandlingStegTilstand
 *     blir oppdatert innenfor samme steg, evt kan man flytte lagring av tilstand ut i en observer som trigges av denne</li>
 *     <li> {@link no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStegOvergangEvent} - når BehandlingStegTilstand
 *     blir oppdatert med en sluttstatus og man går til et annet steg (og noe inkonsekvent ved hopping) </li>
 *     <li> {@link no.nav.foreldrepenger.behandlingskontroll.events.BehandlingTransisjonEvent} - når BehandlingStegTilstand
 *     blir oppdatert som følge av hopp framover </li>
 *     <li> {@link no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStegStatusEvent} - når Behandlingens status endres </li>
 *     <li> {@link no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent} - når Aksjonspunkt oppstår eller endres </li>
 *     <li> {@link no.nav.foreldrepenger.behandlingskontroll.events.BehandlingskontrollEvent} - når prosessering begynner/slutter </li>
 * </ul>
 *
 * <h2>Forbedringer</h2>
 * Det er en rekke muligheter - men noen tiltak peker seg ut som mulige kandidater gitt at man forstår CDI-event-rekkefølge
 * <ul>
 *     <li> Slutte å sende rundt Entitetsobjekter i Events. Første omgang trekke ut info og sende objekt som er definert i modulen </li>
 *     <li> Skille events av intern interesse vs de med ekstern interesse </li>
 *     <li> Se over StegOvergang vs Transisjon og bruken i nesteSteg/hoppFram/hoppTilbake </li>
 *     <li> Rydde opp henleggelse fra meny vs steg - nå publiseres transisjon og en observer kalle behandlingskontroll/henlegg (konvertere) </li>
 *     <li> Tilbakeføring - nå gjøres en del aksjonspunkt-oppdatering før det publiseres egen event. Bedre skille Tilbakeføring/Transisjon </li>
 *     <li> Tilbakeføring til steg - konvertere dagens sak til en TILBAKE-transisjon med målsteg+målstatus (default INNGANG) </li>
 * </ul>
 */
package no.nav.foreldrepenger.behandlingskontroll.events;
